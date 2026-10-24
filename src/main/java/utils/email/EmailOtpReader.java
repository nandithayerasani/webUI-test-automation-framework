package utils.email;

import org.apache.logging.log4j.Logger;
import utils.LoggerUtil;

import java.time.Instant;
import java.util.Optional;

/**
 * Reads a one-time code from a mailbox. Tests use this interface only, so the same test
 * works whether the account receives codes in Gmail, another IMAP mailbox or Outlook.
 *
 *   Instant sent = Instant.now();
 *   click(SEND_CODE_BUTTON);
 *   String otp = EmailOtpReaders.fromConfig().waitForOtp(
 *           OtpQuery.sentAfter(sent).from("no-reply@myapp.com"));
 */
public interface EmailOtpReader {

    long POLL_INTERVAL_MS = 5_000;
    /** Allows for small clock differences between this machine and the mail server. */
    long CLOCK_SKEW_SECONDS = 30;

    /** One look at the mailbox: the code from the newest matching email, if there is one yet. */
    Optional<String> findOtp(OtpQuery query);

    /**
     * Polls the mailbox until an email matching the query arrives, and returns its code.
     *
     * @throws IllegalStateException if no matching email arrives within the query's timeout
     */
    default String waitForOtp(OtpQuery query) {
        Logger log = LoggerUtil.getLogger(getClass());
        Instant deadline = Instant.now().plus(query.timeout());
        log.info("Waiting up to {}s for OTP email", query.timeout().toSeconds());
        while (true) {
            Optional<String> otp = findOtp(query);
            if (otp.isPresent()) {
                log.info("OTP email found");
                return otp.get();
            }
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("No OTP email arrived within " + query.timeout().toSeconds() + "s");
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for OTP email", e);
            }
        }
    }
}
