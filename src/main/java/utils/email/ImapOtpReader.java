package utils.email;

import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.apache.logging.log4j.Logger;
import utils.LoggerUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Reads OTP emails over IMAP. Works with Gmail and any other IMAP mailbox.
 *
 * Gmail setup: turn on 2-Step Verification for the mailbox account, then create an
 * app password (Google Account > Security > App passwords). Use that, not the normal password.
 */
public class ImapOtpReader implements EmailOtpReader {

    private static final Logger LOG = LoggerUtil.getLogger(ImapOtpReader.class);
    /** How many of the newest emails to look at on each poll. */
    private static final int SCAN_LIMIT = 20;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String folderName;

    public ImapOtpReader(String host, int port, String username, String password, String folderName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.folderName = folderName;
    }

    @Override
    public Optional<String> findOtp(OtpQuery query) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "15000");
        props.put("mail.imaps.timeout", "15000");

        Date after = Date.from(query.receivedAfter().minusSeconds(CLOCK_SKEW_SECONDS));
        try (Store store = Session.getInstance(props).getStore("imaps")) {
            store.connect(host, port, username, password);
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            try {
                int count = folder.getMessageCount();
                // Newest first
                for (int i = count; i >= 1 && i > count - SCAN_LIMIT; i--) {
                    Message message = folder.getMessage(i);
                    Date received = message.getReceivedDate();
                    if (received != null && received.before(after)) {
                        break;
                    }
                    if (!query.matches(addresses(message.getFrom()), message.getSubject())) {
                        continue;
                    }
                    Optional<String> otp = OtpExtractor.extract(bodyText(message), query.otpPattern());
                    if (otp.isPresent()) {
                        return otp;
                    }
                }
            } finally {
                folder.close(false);
            }
        } catch (MessagingException | IOException e) {
            LOG.warn("Could not read mailbox: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private static String addresses(Address[] from) {
        return from == null ? "" : Arrays.stream(from).map(Address::toString).collect(Collectors.joining(","));
    }

    /** Returns the text/plain part if there is one, otherwise the HTML part. */
    private static String bodyText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            return String.valueOf(part.getContent());
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String html = null;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part child = multipart.getBodyPart(i);
                if (child.isMimeType("text/plain")) {
                    return String.valueOf(child.getContent());
                }
                String nested = bodyText(child);
                if (nested != null && html == null) {
                    html = nested;
                }
            }
            return html;
        }
        return null;
    }
}
