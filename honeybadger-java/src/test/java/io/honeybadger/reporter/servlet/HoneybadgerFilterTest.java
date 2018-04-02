package io.honeybadger.reporter.servlet;

import com.google.common.collect.ImmutableMap;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.UnitTestExpectedException;
import io.honeybadger.reporter.config.MapConfigContext;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@SuppressWarnings("DefaultCharset")
public class HoneybadgerFilterTest {
    /**
     * Generates a fake instance of a {@link HoneybadgerFilter} for testing.
     */
    private HoneybadgerFilter instance(boolean displayForm) throws ServletException {
        final Map<String, String> props = ImmutableMap.of(
                MapConfigContext.DISPLAY_FEEDBACK_FORM_KEY, String.valueOf(displayForm),
                MapConfigContext.HONEYBADGER_API_KEY, "test-value");
        FilterConfig filterConfig = new FakeFilterConfig(props);
        final HoneybadgerFilter filter = new HoneybadgerFilter();
        filter.init(filterConfig);

        return filter;
    }

    /**
     * Generates a fake instance of a {@link HoneybadgerFilter} for testing.
     * @param reporter reporter instance to use for testing
     */
    private HoneybadgerFilter instance(NoticeReporter reporter, boolean displayForm)
            throws ServletException {
        if (reporter == null) {
            throw new IllegalArgumentException("Reporter must not be null");
        }

        Map<String, String> props = ImmutableMap.of(
                MapConfigContext.DISPLAY_FEEDBACK_FORM_KEY, String.valueOf(displayForm),
                MapConfigContext.HONEYBADGER_API_KEY, "test-value");
        FilterConfig filterConfig = new FakeFilterConfig(props);
        final HoneybadgerFilter filter = new HoneybadgerFilter();
        filter.init(filterConfig);
        filter.setReporter(reporter);

        return filter;
    }


    @Test
    public void filterCanThrowAnError() throws Exception {
        NoticeReporter reporter = mock(NoticeReporter.class);
        HoneybadgerFilter filter = instance(reporter, false);

        FilterChain chain = mock(FilterChain.class);
        Exception exception = new UnitTestExpectedException("Servlet Filter Exception");
        Mockito.doThrow(exception)
                .when(chain).doFilter(any(ServletRequest.class),
                                      any(ServletResponse.class));

        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(System.out));

        boolean thrown = false;

        try {
            filter.doFilter(request, response, chain);
        } catch (UnitTestExpectedException e) {
            thrown = true;
        }

        assertTrue("Expected exception not thrown", thrown);

        verify(reporter, times(1)).reportError(exception, request);
    }

    @Test
    public void filterCanThrowAnErrorToTheHoneybadgerAPI() throws Exception {
        HoneybadgerFilter filter = instance(false);

        FilterChain chain = mock(FilterChain.class);
        Exception exception = new UnitTestExpectedException("Servlet Filter Exception");
        Mockito.doThrow(exception)
                .when(chain).doFilter(any(ServletRequest.class),
                                      any(ServletResponse.class));

        ServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(System.out));

        boolean thrown = false;

        try {
            filter.doFilter(request, response, chain);
        } catch (UnitTestExpectedException e) {
            thrown = true;
        }

        assertTrue("Expected exception not thrown", thrown);
    }
}
