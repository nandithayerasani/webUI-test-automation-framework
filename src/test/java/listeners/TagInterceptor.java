package listeners;

import org.apache.logging.log4j.Logger;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import utils.LoggerUtil;
import utils.TagFilter;

import java.util.List;

/**
 * Applies -Dtags=... before the run starts. Registered in testng.xml.
 *
 * A test runs when one of its groups matches a tag. Data-driven tests are kept too, and
 * TestDataReader then hands them only the Excel rows whose TestCaseId matches.
 */
public class TagInterceptor implements IMethodInterceptor {

    private static final Logger LOG = LoggerUtil.getLogger(TagInterceptor.class);

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        if (!TagFilter.isFiltering()) {
            return methods;
        }
        List<IMethodInstance> selected = methods.stream()
                .filter(m -> keep(m.getMethod()))
                .toList();
        LOG.info("Tags {} selected {} of {} test methods in '{}'",
                TagFilter.activeTags(), selected.size(), methods.size(), context.getName());
        return selected;
    }

    private static boolean keep(ITestNGMethod method) {
        return TagFilter.matches(method.getGroups()) || method.isDataDriven();
    }
}
