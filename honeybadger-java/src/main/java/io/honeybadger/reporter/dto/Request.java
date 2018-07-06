package io.honeybadger.reporter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing the properties of an HTTP request that triggered an
 * error.
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Request implements Serializable {
    private static final long serialVersionUID = 9105532956022860986L;

    private final Context context;
    private final String url;
    private final Params params;
    private final Session session;
    @JsonProperty("cgi_data")
    private CgiData cgiData;

    @JsonCreator
    public Request(@JsonProperty("context")final Context context, @JsonProperty("url") final String url,
                   @JsonProperty("params") final Params params,  @JsonProperty("session") final Session session,
                   @JsonProperty("cgi_data") final CgiData cgiData) {
        this.context = context;
        this.url = url;
        this.params = params;
        this.session = session;
        this.cgiData = cgiData;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(getContext(), request.getContext()) &&
                Objects.equals(getUrl(), request.getUrl()) &&
                Objects.equals(getParams(), request.getParams()) &&
                Objects.equals(getSession(), request.getSession()) &&
                Objects.equals(getCgiData(), request.getCgiData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContext(), getUrl(), getParams(), getSession(), getCgiData());
    }

    public String getUrl() {
        return url;
    }

    public Params getParams() {
        return params;
    }

    public Session getSession() {
        return session;
    }

    public CgiData getCgiData() {
        return cgiData;
    }

    public void setCgiData(final CgiData value) {
        this.cgiData = value;
    }

    public Context getContext() {
        return context;
    }
}
