package io.honeybadger.reporter.dto;

import io.honeybadger.reporter.config.ConfigContext;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing an error that is reported to the Honeybadger API.
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
public class Notice implements Serializable {
    private static final long serialVersionUID = 1661111694538362413L;

    private final ConfigContext config;

    private Notifier notifier = new Notifier();
    private ServerDetails server;
    private Details details;
    // This is defined as serializable so that it can use APIs that the
    // implementers may not have available like the Servlet API
    private Request request;
    private NoticeDetails error;

    public Notice(ConfigContext config) {
        this.config = config;
        this.server = new ServerDetails(config);
        this.details = new Details(this.config);
        this.details.addDefaultDetails();
    }
    //TODO: Elijah says to remove. But will break compatibility, so we should soft deprecate in a 1.x branch
    public Notice() {
        ConfigContext config = ConfigContext.THREAD_LOCAL.get();
        if (config == null) throw new NullPointerException(
                "Unable to get the expected ConfigContext from ThreadLocal");

        this.config = config;
        this.server = new ServerDetails(config);
    }

    public Notifier getNotifier() {
        return notifier;
    }

    public Notice setNotifier(Notifier notifier) {
        this.notifier = notifier;
        return this;
    }

    public NoticeDetails getError() {
        return error;
    }

    public Notice setError(NoticeDetails error) {
        this.error = error;
        return this;
    }

    public Notice setServer(ServerDetails server) {
        this.server = server;
        return this;
    }

    public ServerDetails getServer() {
        return server;
    }

    public Details getDetails() {
        return details;
    }

    public Notice setDetails(Details details) {
        this.details = details;
        return this;
    }

    public Request getRequest() {
        return request;
    }

    public Notice setRequest(Request request) {
        this.request = request;
        return this;
    }

    @Override
    public boolean equals(Object o) {
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
}
