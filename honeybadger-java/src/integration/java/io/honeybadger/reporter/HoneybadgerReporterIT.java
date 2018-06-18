package io.honeybadger.reporter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.honeybadger.loader.HoneybadgerNoticeLoader;
import io.honeybadger.reporter.config.ConfigContext;
import io.honeybadger.reporter.config.SystemSettingsConfigContext;
import io.honeybadger.reporter.dto.CgiData;
import io.honeybadger.reporter.dto.Notice;
import io.honeybadger.reporter.dto.Request;
import io.honeybadger.reporter.servlet.FakeHttpServletRequest;
import org.apache.http.HttpHeaders;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HoneybadgerReporterIT {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final HoneybadgerNoticeLoader loader;
    private final NoticeReporter reporter;

    public HoneybadgerReporterIT() {
        ConfigContext config = new SystemSettingsConfigContext();
        if (config.getApiKey() == null) {
            throw new IllegalArgumentException("API key must be specified");
        }

        config.getExcludedClasses().add(UnsupportedOperationException.class.getName());
        config.getExcludedClasses().add(IllegalArgumentException.class.getName());

        this.loader = new HoneybadgerNoticeLoader(config);
        this.reporter = new HoneybadgerReporter(config);
    }

    @Test
    public void willReportErrorWithRequest() throws Exception {
        MDC.put("testValue", "something");

        Throwable cause = new RuntimeException("I'm the cause");
        Throwable t = new UnitTestExpectedException("Test exception " +
                System.currentTimeMillis(), cause);

        ArrayList<String> cookies = new ArrayList<>(ImmutableList.of(
                "theme=light",
                "sessionToken=abc123; Expires=Wed, 09 Jun 2021 10:18:14 GMT",
                "multi-value=true; lastItem=true"));

        Map<String, ? extends List<String>> headers = ImmutableMap.of(
                HttpHeaders.REFERER, ImmutableList.of("Tester"),
                HttpHeaders.USER_AGENT, ImmutableList.of("User Agent"),
                HttpHeaders.ACCEPT, ImmutableList.of("text/html"),
                "Set-Cookie", cookies
        );

        HttpServletRequest request = new FakeHttpServletRequest(headers);

        NoticeReportResult result = reporter.reportError(t, request);
        assertNotNull("Result of error report should never be null", result);

        UUID id = result.getId();

        logger.info("Error ID returned from Honeybadger is: {}", id);

        assertNotNull("Didn't send error correctly to Honeybadger API", id);

        logger.info("Created error with id: {}", id);

        // Wait for the Honeybadger API to process the error
        Thread.sleep(20000);

        try {
            Notice error = loader.findErrorDetails(id);
            assertReportedErrorIsSame(result.getNotice(), error);
        } catch (IllegalArgumentException e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void willSuppressExcludedExceptionClasses() throws Exception {
        final Exception error = new UnsupportedOperationException(
                "I should be suppressed");
        final NoticeReportResult result = reporter.reportError(error);

        assertNull("A suppressed error was actually added", result);
    }

    static void assertReportedErrorIsSame(Notice expected, Notice actual) {
        // Right now details are supported, but the retrieval API is not,
        // so we don't check them
//        if (!expected.getDetails().equals(actual.getDetails())) {
//            fail(String.format("Details were not equal.\n" +
//                    "Expected: %s\n" +
//                    "Actual:   %s",
//                    expected.getDetails(), actual.getDetails()));
//        }

        assertEquals(expected.getNotifier(), actual.getNotifier());

        // Because this is not retrieved by the API the V1 check is not a particularly useful
        // validation. It only verifies that we return the same data that we put in,
        // but memory and load data is automatically generated at construction time, so
        // we'd have to store this context before verifying it.
        //
        // Accordingly, instead of this, we're doing some more narrow validation
        //        assertEquals(expected.getServer(), actual.getServer());
        assertEquals(expected.getServer().getHostname(), actual.getServer().getHostname());
        assertEquals(expected.getServer().getEnvironmentName(), actual.getServer().getEnvironmentName());

        Request expectedRequest = expected.getRequest();
        Request actualRequest = actual.getRequest();

        assertEquals(expectedRequest.getContext(), actualRequest.getContext());
        assertEquals(expectedRequest.getParams(), actualRequest.getParams());
        assertEquals(expectedRequest.getSession(), actualRequest.getSession());

        CgiData expectedCgi = expectedRequest.getCgiData();
        CgiData actualCgi = actualRequest.getCgiData();

        assertEquals(expectedCgi.getAsInteger(HttpHeaders.CONTENT_LENGTH.toUpperCase()),
                     actualCgi.getAsInteger(HttpHeaders.CONTENT_LENGTH.toUpperCase()));

        assertEquals(expectedCgi.getAsInteger("SERVER_PORT"),
                actualCgi.getAsInteger("SERVER_PORT"));

        assertEquals(expectedCgi.getAsInteger("REMOTE_PORT"),
                actualCgi.getAsInteger("REMOTE_PORT"));

        assertEquals(expectedCgi.get(HttpHeaders.CONTENT_TYPE.toUpperCase()),
                     actualCgi.get(HttpHeaders.CONTENT_TYPE.toUpperCase()));

        assertEquals(expectedCgi.get(HttpHeaders.ACCEPT.toUpperCase()),
                actualCgi.get(HttpHeaders.ACCEPT.toUpperCase()));

        assertEquals(expectedCgi.get("REQUEST_METHOD"),
                actualCgi.get("REQUEST_METHOD"));

        assertEquals(expectedCgi.get(HttpHeaders.USER_AGENT.toUpperCase()),
                actualCgi.get(HttpHeaders.USER_AGENT.toUpperCase()));

        assertEquals(expectedCgi.get("HTTP_COOKIE"),
                actualCgi.get("HTTP_COOKIE"));

        assertEquals(expectedCgi.get("SERVER_NAME"),
                actualCgi.get("SERVER_NAME"));

//        Note: We are waiting on an API that returns everything in order
//              for us to test this properly
//        ErrorDetails expectedError = expected.getError();
//        ErrorDetails actualError = actual.getError();
//
//        assertEquals(expectedError, actualError);
    }
}
