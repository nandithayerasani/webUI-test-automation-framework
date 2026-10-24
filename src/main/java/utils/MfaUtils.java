package utils;

import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Locale;

/**
 * Generates the same 6-digit codes as Google Authenticator / Microsoft Authenticator
 * (TOTP, RFC 6238), so tests can log in to accounts that have MFA turned on.
 *
 * Setup: when enrolling the test account in MFA, choose "can't scan the QR code" to see the
 * secret key (a Base32 string such as JBSWY3DPEHPK3PXP). Store it in the MFA_SECRET environment
 * variable or a pipeline secret. Never commit it.
 *
 * Usage in a page:
 *   type(MFA_CODE_INPUT, MfaUtils.getCode());
 */
public final class MfaUtils {

    private static final Logger LOG = LoggerUtil.getLogger(MfaUtils.class);
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** If fewer seconds than this are left in the current 30s window, wait for the next code. */
    private static final int MIN_SECONDS_LEFT = 5;

    private MfaUtils() {
    }

    /** Code for the secret in the MFA_SECRET environment variable, or mfa.secret in config. */
    public static String getCode() {
        return getCode(ConfigReader.getSecret("MFA_SECRET", "mfa.secret"));
    }

    /**
     * Code for the given Base32 secret. If the current code is about to expire it waits for the
     * next one, so the code is still valid when the form is submitted.
     */
    public static String getCode(String base32Secret) {
        long now = Instant.now().getEpochSecond();
        long secondsLeft = PERIOD_SECONDS - (now % PERIOD_SECONDS);
        if (secondsLeft < MIN_SECONDS_LEFT) {
            LOG.info("MFA code expires in {}s, waiting for the next one", secondsLeft);
            sleep(secondsLeft * 1000 + 200);
            now = Instant.now().getEpochSecond();
        }
        return generate(base32Secret, now, DIGITS);
    }

    /** Pure TOTP calculation, exposed for testing. */
    public static String generate(String base32Secret, long epochSeconds, int digits) {
        byte[] key = decodeBase32(base32Secret);
        long counter = epochSeconds / PERIOD_SECONDS;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not generate MFA code", e);
        }
    }

    private static byte[] decodeBase32(String secret) {
        String clean = secret.replace(" ", "").replace("-", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : clean.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("MFA secret is not valid Base32: " + c);
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.put((byte) (buffer >> (bitsLeft - 8)));
                bitsLeft -= 8;
            }
        }
        return out.array();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
