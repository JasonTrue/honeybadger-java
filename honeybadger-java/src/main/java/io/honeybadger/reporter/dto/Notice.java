package io.honeybadger.reporter.dto;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.honeybadger.reporter.config.ConfigContext;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing an error that is reported to the Honeybadger API.
 *
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"environment", "created_at", "message", "token", "fault_id",
        "application_trace", "backtrace", "deploy", "url"})
public class Notice implements Serializable {
    private static final long serialVersionUID = 1661111694538362413L;
    private Long id = null;
    private final ConfigContext config;

    private Notifier notifier = new Notifier();
    private ServerDetails server;
    private Details details;
    // This is defined as serializable so that it can use APIs that the
    // implementers may not have available like the Servlet API
    private Request request;
    private NoticeDetails error;

    public Notice(final ConfigContext config) {
        this.config = config;
        this.server = new ServerDetails(config);
        this.details = new Details(this.config);
        this.details.addDefaultDetails();
    }

    @JsonCreator
    public Notice(@JacksonInject("config") final ConfigContext config,
                  @JsonProperty("id") final Long id,
                  @JsonProperty("web_environment") final CgiData webEnvironment,
                  @JsonProperty("request") final Request request) {
        this.config = config;
        this.id = id;
        this.server = new ServerDetails(config);
        this.request = request;
        this.request.setCgiData(webEnvironment);
    }

    public Notifier getNotifier() {
        return notifier;
    }

    public Notice setNotifier(final Notifier notifier) {
        this.notifier = notifier;
        return this;
    }

    public NoticeDetails getError() {
        return error;
    }

    public Notice setError(final NoticeDetails error) {
        this.error = error;
        return this;
    }

    public Notice setServer(final ServerDetails server) {
        this.server = server;
        return this;
    }

    public ServerDetails getServer() {
        return server;
    }

    public Details getDetails() {
        return details;
    }

    public Notice setDetails(final Details details) {
        this.details = details;
        return this;
    }

    public Request getRequest() {
        return request;
    }

    public Notice setRequest(final Request request) {
        this.request = request;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notice notice = (Notice) o;
        return Objects.equals(config, notice.config) &&
                Objects.equals(notifier, notice.notifier) &&
                Objects.equals(server, notice.server) &&
                Objects.equals(details, notice.details) &&
                Objects.equals(request, notice.request) &&
                Objects.equals(error, notice.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, notifier, server, details, request, error);
    }

    public Long getId() {
        return id;
    }
}
