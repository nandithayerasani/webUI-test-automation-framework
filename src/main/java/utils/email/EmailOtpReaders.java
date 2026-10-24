package utils.email;

import utils.ConfigReader;

import java.util.Locale;

/**
 * Builds the mailbox reader chosen by "email.provider" in config.properties:
 *   imap  - Gmail or any IMAP mailbox (EMAIL_PASSWORD)
 *   graph - Outlook / Microsoft 365 (GRAPH_TENANT_ID, GRAPH_CLIENT_ID, GRAPH_CLIENT_SECRET)
 * Passwords and secrets are read from environment variables, never from the repo.
 */
public final class EmailOtpReaders {

    private EmailOtpReaders() {
    }

    public static EmailOtpReader fromConfig() {
        String provider = ConfigReader.get("email.provider", "imap").toLowerCase(Locale.ROOT);
        switch (provider) {
            case "imap":
                return new ImapOtpReader(
                        ConfigReader.get("email.imap.host", "imap.gmail.com"),
                        ConfigReader.getInt("email.imap.port", 993),
                        ConfigReader.get("email.username"),
                        ConfigReader.getSecret("EMAIL_PASSWORD", "EMAIL_PASSWORD"),
                        ConfigReader.get("email.folder", "INBOX"));
            case "graph":
                return new GraphOtpReader(
                        ConfigReader.getSecret("GRAPH_TENANT_ID", "GRAPH_TENANT_ID"),
                        ConfigReader.getSecret("GRAPH_CLIENT_ID", "GRAPH_CLIENT_ID"),
                        ConfigReader.getSecret("GRAPH_CLIENT_SECRET", "GRAPH_CLIENT_SECRET"),
                        ConfigReader.get("email.username"),
                        ConfigReader.get("email.folder", "inbox"));
            default:
                throw new IllegalArgumentException("Unsupported email.provider: " + provider);
        }
    }
}
