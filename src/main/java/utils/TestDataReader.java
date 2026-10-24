package utils;

import constants.FrameworkConstants;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.testng.ITestNGMethod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Reads test data from the Excel workbook of the current environment,
 * set by "testdata.file" in environments/<env>.properties (e.g. qa/TestData.xlsx).
 *
 * Sheet layout: one sheet per feature, first row is the column headers, one row per test case.
 * Two columns have a special meaning:
 *   TestCaseId - used by getRow to find a row
 *   Run        - rows with N are skipped (leave blank or Y to run)
 *
 * Every value is returned as a String exactly as shown in Excel.
 */
public final class TestDataReader {

    private static final Logger LOG = LoggerUtil.getLogger(TestDataReader.class);
    private static final String ID_COLUMN = "TestCaseId";
    private static final String RUN_COLUMN = "Run";
    private static final Map<String, List<Map<String, String>>> CACHE = new ConcurrentHashMap<>();

    private TestDataReader() {
    }

    /** All runnable rows of a sheet, each as column name -> value. */
    public static List<Map<String, String>> getRows(String sheetName) {
        return CACHE.computeIfAbsent(sheetName, TestDataReader::load);
    }

    /** Runnable rows that match a filter, e.g. row -> "invalid".equals(row.get("Scenario")). */
    public static List<Map<String, String>> getRows(String sheetName, Predicate<Map<String, String>> filter) {
        return getRows(sheetName).stream().filter(filter).toList();
    }

    /** The row with the given TestCaseId. */
    public static Map<String, String> getRow(String sheetName, String testCaseId) {
        return getRows(sheetName).stream()
                .filter(row -> testCaseId.equalsIgnoreCase(row.get(ID_COLUMN)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No row with " + ID_COLUMN + "=" + testCaseId + " in sheet " + sheetName));
    }

    /**
     * Rows as a TestNG data provider: each test invocation receives one Map.
     * Pass the ITestNGMethod TestNG injects so -Dtags can pick single rows by TestCaseId:
     *
     *   @DataProvider(name = "invalidLogins")
     *   public Object[][] invalidLogins(ITestNGMethod method) {
     *       return TestDataReader.asDataProvider(method, "Login", row -> "invalid".equals(row.get("Scenario")));
     *   }
     *
     * With -Dtags set, all rows are used if the test method's own groups match a tag;
     * otherwise only rows whose TestCaseId matches a tag.
     */
    public static Object[][] asDataProvider(ITestNGMethod method, String sheetName,
                                            Predicate<Map<String, String>> filter) {
        boolean methodSelected = method == null || TagFilter.matches(method.getGroups());
        return getRows(sheetName, filter).stream()
                .filter(row -> methodSelected || TagFilter.matches(row.getOrDefault(ID_COLUMN, "")))
                .map(row -> new Object[]{row})
                .toArray(Object[][]::new);
    }

    public static Object[][] asDataProvider(ITestNGMethod method, String sheetName) {
        return asDataProvider(method, sheetName, row -> true);
    }

    private static List<Map<String, String>> load(String sheetName) {
        Path file = FrameworkConstants.TEST_DATA_DIR.resolve(ConfigReader.get("testdata.file"));
        try (InputStream in = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + file);
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            List<String> columns = new ArrayList<>();
            for (Cell cell : header) {
                columns.add(formatter.formatCellValue(cell).trim());
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < columns.size(); c++) {
                    String value = formatter.formatCellValue(row.getCell(c)).trim();
                    empty &= value.isEmpty();
                    values.put(columns.get(c), value);
                }
                if (!empty && !"N".equalsIgnoreCase(values.getOrDefault(RUN_COLUMN, ""))) {
                    rows.add(Collections.unmodifiableMap(values));
                }
            }
            LOG.info("Loaded {} rows from sheet {}", rows.size(), sheetName);
            return Collections.unmodifiableList(rows);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read test data file " + file, e);
        }
    }
}
