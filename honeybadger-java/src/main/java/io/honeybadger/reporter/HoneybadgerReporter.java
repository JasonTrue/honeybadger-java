package io.honeybadger.reporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.honeybadger.reporter.config.ConfigContext;
import io.honeybadger.reporter.config.SystemSettingsConfigContext;
import io.honeybadger.reporter.dto.HttpServletRequestFactory;
import io.honeybadger.reporter.dto.Notice;
import io.honeybadger.reporter.dto.NoticeDetails;
import io.honeybadger.reporter.dto.PlayHttpRequestFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

/**
 * Reporter utility class that gives a simple interface for sending Java
 * {@link java.lang.Throwable} classes to the Honeybadger API.
 *
 * @author <a href="https://github.com/page1">page1</a>
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.0
 */
public class HoneybadgerReporter implements NoticeReporter {
    private static Class<?> EXCEPTION_CONTEXT_CLASS;

    static {
        try {
            EXCEPTION_CONTEXT_CLASS = Class.forName("org.apache.commons.lang3.exception.ExceptionContext");
        } catch (ClassNotFoundException e) {
            EXCEPTION_CONTEXT_CLASS = null;
        }
    }

    protected ConfigContext config;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new GsonBuilder()
            .setExclusionStrategies(new HoneybadgerExclusionStrategy())
            .create();

    public HoneybadgerReporter() {
        this(new SystemSettingsConfigContext());
    }

    public HoneybadgerReporter(ConfigContext config) {
        this.config = config;

        if (config.getApiKey() == null) {
            throw new IllegalArgumentException("API key must be set");
        }

        if (config.getApiKey().isEmpty()) {
            throw new IllegalArgumentException("API key must not be empty");
        }

        if (config.getHoneybadgerUrl() == null) {
            throw new IllegalArgumentException("Honeybadger URL must be set");
        }
    }

    /**
     * Send any Java {@link java.lang.Throwable} to the Honeybadger error
     * reporting interface.
     *
     * @param error error to report
     * @return UUID of error created, if there was a problem null
     */
    @Override
    public NoticeReportResult reportError(Throwable error) {
        return submitError(error, null);
    }

    /**
     * Send any Java {@link java.lang.Throwable} to the Honeybadger error
     * reporting interface.
     *
     * Currently only {@link javax.servlet.http.HttpServletRequest} objects
     * are supported as request properties.
     *
     * @param error error to report
     * @param request Object to parse for request properties
     * @return UUID of error created, if there was a problem or ignored null
     */
    @Override
    public NoticeReportResult reportError(Throwable error, Object request) {
        if (error == null) { return null; }
        if (request == null) { return submitError(error, null); }

        final io.honeybadger.reporter.dto.Request requestDetails;

        // CUSTOM USAGE OF REQUEST DTO
        if (request instanceof io.honeybadger.reporter.dto.Request) {
            logger.debug("Reporting using a request DTO");
            requestDetails = (io.honeybadger.reporter.dto.Request)request;

        // SERVLET REQUEST - ALSO USED BY SPRING
        } else if (supportsHttpServletRequest() && request instanceof javax.servlet.http.HttpServletRequest)  {
            logger.debug("Reporting from a servlet context");
            requestDetails =  HttpServletRequestFactory.create(config,
                    (javax.servlet.http.HttpServletRequest) request);

        // PLAY FRAMEWORK REQUEST
        } else if (supportsPlayHttpRequest() && request instanceof play.mvc.Http.Request) {
            logger.debug("Reporting from the Play Framework");
            requestDetails = PlayHttpRequestFactory.create(config,
                    (play.mvc.Http.Request)request);
        } else {
            logger.debug("No request object available");
            requestDetails = null;
        }

        return submitError(error, requestDetails);
    }

    @Override
    public ConfigContext getConfig() {
        return config;
    }

    protected boolean supportsHttpServletRequest() {
        try {
            Class.forName("javax.servlet.http.HttpServletRequest");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    protected boolean supportsPlayHttpRequest() {
        try {
            Class.forName("play.mvc.Http", false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    protected NoticeReportResult submitError(
            final Throwable error, final io.honeybadger.reporter.dto.Request request) {
        final String errorClassName = error.getClass().getName();
        if (errorClassName != null &&
                config.getExcludedClasses().contains(errorClassName)) {
            return null;
        }

        final Notice notice = new Notice(config);

        if (request != null) {
            final String message = parseMessage(error);
            NoticeDetails noticeDetails = new NoticeDetails(
                    config, error, Collections.emptySet(), message);
            notice.setRequest(request).setError(noticeDetails);
        } else {
            NoticeDetails noticeDetails = new NoticeDetails(config, error);
            notice.setError(noticeDetails);
        }

        for (int retries = 0; retries < 3; retries++) {
            try {
                String json = gson.toJson(notice);
                HttpResponse response = sendToHoneybadger(json)
                        .returnResponse();
                int responseCode = response.getStatusLine().getStatusCode();

                if (responseCode != 201)
                    logger.error("Honeybadger did not respond with the " +
                                 "correct code. Response was [{}]. Retries={}",
                                 responseCode, retries);
                else {
                    UUID id = parseErrorId(response, gson);

                    return new NoticeReportResult(id, notice, error);
                }
            } catch (IOException e) {
                String msg = String.format("There was an error when trying " +
                                           "to send the error to " +
                                           "Honeybadger. Retries=%d", retries);
                logger.error(msg, e);
                logger.error("Original Error", error);
                return null;
            }
        }

        return null;
    }

    private UUID parseErrorId(HttpResponse response, Gson gson)
            throws IOException {
        try (InputStream in = response.getEntity().getContent();
             Reader reader = new InputStreamReader(in)) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> map =
                    (HashMap<String, String>)gson.fromJson(reader, HashMap.class);

            if (map.containsKey("id")) {
                return UUID.fromString(map.get("id"));
            } else {
                return null;
            }
        }
    }

    /**
     * Parses the exception message and strips out redundant context information
     * if we are already sending the information as part of the error context.
     *
     * @param throwable throwable to parse message from
     * @return string containing the throwable's error message
     */
    private static String parseMessage(final Throwable throwable) {
        if (EXCEPTION_CONTEXT_CLASS == null) {
            return throwable.getMessage();
        }

        if (exceptionClassHasContextedVariables(throwable.getClass())) {
            final String msg = throwable.getMessage();
            final int contextSeparatorPos = msg.indexOf("Exception Context:");

            if (contextSeparatorPos == -1) {
                return msg;
            }

            return msg.substring(0, contextSeparatorPos).trim();
        } else {
            return throwable.getMessage();
        }
    }

    /**
     * Send an error encoded in JSON to the Honeybadger API.
     *
     * @param jsonError Error JSON payload
     * @return Status code from the Honeybadger API
     * @throws IOException thrown when a network was encountered
     */
    private Response sendToHoneybadger(String jsonError) throws IOException {
        URI honeybadgerUrl = URI.create(
                String.format("%s/%s", config.getHoneybadgerUrl(), "v1/notices"));
        Request request = buildRequest(honeybadgerUrl, jsonError);

        return request.execute();
    }

    /**
     * Builds a Apache HTTP Client request object configured for calling the
     * Honeybadger API.
     *
     * @param honeybadgerUrl Endpoint location
     * @param jsonError Error JSON payload
     * @return a configured request object
     */
    private Request buildRequest(URI honeybadgerUrl, String jsonError) {
        Request request = Request
               .Post(honeybadgerUrl)
               .addHeader("X-API-Key", config.getApiKey())
               .addHeader("Accept", "application/json")
               .version(HttpVersion.HTTP_1_1)
               .bodyString(jsonError, ContentType.APPLICATION_JSON);

        if (System.getProperty("http.proxyHost") != null &&
            !System.getProperty("http.proxyHost").isEmpty()) {
            int port = Integer.parseInt(System.getProperty("http.proxyPort"));
            HttpHost proxy = new HttpHost(System.getProperty("http.proxyHost"),
                                          port);

            request.viaProxy(proxy);
        }

        return request;
    }

    /**
     * Tests to see if a given exception class has embedded context variables
     * like {@link org.apache.commons.lang3.exception.ContextedException}.
     *
     * @param clazz class to compare
     * @return true if a contexted exception, otherwise false
     */
    private static boolean exceptionClassHasContextedVariables(final Class<?> clazz) {
        if (EXCEPTION_CONTEXT_CLASS == null) {
            return false;
        }

        return EXCEPTION_CONTEXT_CLASS.isAssignableFrom(clazz);
    }
}
