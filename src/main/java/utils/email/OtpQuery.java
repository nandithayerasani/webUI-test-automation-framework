package utils.email;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Describes which email holds the code. Only emails received after {@code receivedAfter}
 * are considered, so an old code from a previous run is never picked up.
 *
 *   OtpQuery query = OtpQuery.sentAfter(Instant.now())
 *           .from("no-reply@myapp.com")
 *           .subjectContains("verification code");
 */
public final class OtpQuery {

    /** Default: the first standalone 4 to 8 digit number in the email. */
    public static final Pattern DEFAULT_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b");

    private final Instant receivedAfter;
    private String fromContains;
    private String subjectContains;
    private Pattern otpPattern = DEFAULT_PATTERN;
    private Duration timeout = Duration.ofSeconds(60);

    private OtpQuery(Instant receivedAfter) {
        this.receivedAfter = receivedAfter;
    }

    /** Capture Instant.now() just before the action that sends the email. */
    public static OtpQuery sentAfter(Instant receivedAfter) {
        return new OtpQuery(receivedAfter);
    }

    public OtpQuery from(String fromContains) {
        this.fromContains = fromContains;
        return this;
    }

    public OtpQuery subjectContains(String subjectContains) {
        this.subjectContains = subjectContains;
        return this;
    }

    /** Regex whose first group is the code, e.g. "code is (\\d{6})". */
    public OtpQuery otpPattern(String regex) {
        this.otpPattern = Pattern.compile(regex);
        return this;
    }

    public OtpQuery timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Instant receivedAfter() {
        return receivedAfter;
    }

    public String fromContains() {
        return fromContains;
    }

    public String subjectContains() {
        return subjectContains;
    }

    public Pattern otpPattern() {
        return otpPattern;
    }

    public Duration timeout() {
        return timeout;
    }

    /** True if the sender and subject match this query. Case-insensitive; null filters match anything. */
    boolean matches(String from, String subject) {
        return containsIgnoreCase(from, fromContains) && containsIgnoreCase(subject, subjectContains);
    }

    private static boolean containsIgnoreCase(String value, String part) {
        if (part == null || part.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(part.toLowerCase());
    }
}
