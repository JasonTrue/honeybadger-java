package io.honeybadger.reporter;

/**
 * Exception class used to send exceptions to the remote HoneyBadger API
 * for use with unit tests.
 *
 * @author @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.9
 */
public class UnitTestExpectedException extends RuntimeException {
    public UnitTestExpectedException() {
    }

    public UnitTestExpectedException(String message) {
        super(message);
    }

    public UnitTestExpectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnitTestExpectedException(Throwable cause) {
        super(cause);
    }

    public UnitTestExpectedException(String message, Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
