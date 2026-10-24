package utils.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import utils.LoggerUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Reads OTP emails from Outlook / Microsoft 365 through Microsoft Graph.
 * (Microsoft no longer allows password logins over IMAP.)
 *
 * Setup, done once by an Azure admin:
 *   1. Entra ID > App registrations > New registration.
 *   2. API permissions > Microsoft Graph > Application permissions > Mail.Read > Grant admin consent.
 *      Ideally limit the app to the test mailbox with an Exchange application access policy.
 *   3. Certificates &amp; secrets > New client secret.
 * Then set GRAPH_TENANT_ID, GRAPH_CLIENT_ID and GRAPH_CLIENT_SECRET as environment variables.
 *
 * Works with work/school (Microsoft 365) mailboxes. Personal outlook.com accounts are not
 * supported by app-only access.
 */
public class GraphOtpReader implements EmailOtpReader {

    private static final Logger LOG = LoggerUtil.getLogger(GraphOtpReader.class);
    private static final String GRAPH = "https://graph.microsoft.com/v1.0";

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String mailbox;
    private final String folder;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper json = new ObjectMapper();

    private String accessToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public GraphOtpReader(String tenantId, String clientId, String clientSecret, String mailbox, String folder) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.mailbox = mailbox;
        this.folder = folder;
    }

    @Override
    public Optional<String> findOtp(OtpQuery query) {
        String since = DateTimeFormatter.ISO_INSTANT.format(
                query.receivedAfter().minusSeconds(CLOCK_SKEW_SECONDS).truncatedTo(ChronoUnit.SECONDS));
        String url = GRAPH + "/users/" + encode(mailbox) + "/mailFolders/" + encode(folder) + "/messages"
                + "?$filter=" + encode("receivedDateTime ge " + since)
                + "&$orderby=" + encode("receivedDateTime desc")
                + "&$top=20&$select=subject,from,body,receivedDateTime";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + token())
                    .header("Prefer", "outlook.body-content-type=\"text\"")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("Graph returned {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            for (JsonNode message : json.readTree(response.body()).path("value")) {
                String from = message.path("from").path("emailAddress").path("address").asText("");
                String subject = message.path("subject").asText("");
                if (!query.matches(from, subject)) {
                    continue;
                }
                Optional<String> otp = OtpExtractor.extract(message.path("body").path("content").asText(""),
                        query.otpPattern());
                if (otp.isPresent()) {
                    return otp;
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not read mailbox: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for OTP email", e);
        }
        return Optional.empty();
    }

    /** Client-credentials token, reused until shortly before it expires. */
    private String token() throws IOException, InterruptedException {
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }
        String form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&scope=" + encode("https://graph.microsoft.com/.default")
                + "&grant_type=client_credentials";
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://login.microsoftonline.com/" + encode(tenantId) + "/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Could not get Microsoft Graph token (" + response.statusCode()
                    + "). Check GRAPH_TENANT_ID, GRAPH_CLIENT_ID and GRAPH_CLIENT_SECRET.");
        }
        JsonNode body = json.readTree(response.body());
        accessToken = body.path("access_token").asText();
        tokenExpiry = Instant.now().plusSeconds(body.path("expires_in").asLong(3600) - 120);
        return accessToken;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
