package org.bf2.cos.fleetshard.operator.client;

import java.net.ConnectException;
import java.util.concurrent.Callable;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.bf2.cos.fleetshard.support.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaClientHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaClientHelper.class);

    public static <T extends Throwable> void run(ThrowingRunnable<T> runnable) {
        try {
            runnable.run();
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();
            final String error = response.readEntity(String.class);

            LOGGER.warn("error={}, status={}, message={}",
                error,
                response.getStatus(),
                e.getMessage());

            throw new MetaClientException(e, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new MetaClientException(e.getMessage());
            } else {
                throw new MetaClientException(e);
            }
        } catch (Throwable e) {
            throw new MetaClientException(e);
        }
    }

    public static <T> T call(Callable<T> callable) {
        try {
            return callable.call();
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();
            final String error = response.readEntity(String.class);

            LOGGER.warn("error={}, status={}, message={}",
                error,
                response.getStatus(),
                e.getMessage());

            throw new MetaClientException(e, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new MetaClientException(e.getMessage());
            } else {
                throw new MetaClientException(e);
            }
        } catch (Throwable e) {
            throw new MetaClientException(e);
        }
    }
}
