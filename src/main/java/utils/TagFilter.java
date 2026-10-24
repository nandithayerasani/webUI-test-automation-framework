package utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides which tests run when "tags" is set, e.g.
 *   mvn test -Dtags=nanditha-0345
 *   mvn test -Dtags=nanditha-0345,nanditha-0346
 *   mvn test -Dtags=smoke
 *
 * A tag is matched against a test's TestNG groups and, for data-driven tests, against the
 * TestCaseId of each Excel row. Matching ignores case and a leading "@".
 * When "tags" is empty, everything runs.
 */
public final class TagFilter {

    /** Shape of a Jira key such as nanditha-0345 or PROJ-12. Used to show IDs in reports. */
    private static final Pattern JIRA_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9]*-\\d+");

    private TagFilter() {
    }

    /** Tags requested for this run, normalised. Empty set means run everything. */
    public static Set<String> activeTags() {
        return Arrays.stream(ConfigReader.get("tags", "").split(","))
                .map(TagFilter::normalise)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isFiltering() {
        return !activeTags().isEmpty();
    }

    /** True if no filter is set, or any of the values matches a requested tag. */
    public static boolean matches(String... values) {
        Set<String> tags = activeTags();
        return tags.isEmpty() || Arrays.stream(values).map(TagFilter::normalise).anyMatch(tags::contains);
    }

    public static boolean isJiraKey(String value) {
        return value != null && JIRA_KEY.matcher(normalise(value)).matches();
    }

    private static String normalise(String tag) {
        String t = tag == null ? "" : tag.trim();
        if (t.startsWith("@")) {
            t = t.substring(1);
        }
        return t.toLowerCase(Locale.ROOT);
    }
}
