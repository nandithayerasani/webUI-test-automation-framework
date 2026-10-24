package utils.email;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pulls the code out of an email body, plain text or HTML. */
public final class OtpExtractor {

    private static final Pattern STYLE_OR_SCRIPT = Pattern.compile("(?is)<(style|script)[^>]*>.*?</\\1>");
    private static final Pattern TAGS = Pattern.compile("(?s)<[^>]+>");

    private OtpExtractor() {
    }

    public static Optional<String> extract(String body, Pattern pattern) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(toPlainText(body));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group());
    }

    /** Strips HTML so codes split across tags or hidden in CSS colours (#123456) are handled. */
    static String toPlainText(String body) {
        String text = STYLE_OR_SCRIPT.matcher(body).replaceAll(" ");
        text = TAGS.matcher(text).replaceAll(" ");
        return text.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }
}
