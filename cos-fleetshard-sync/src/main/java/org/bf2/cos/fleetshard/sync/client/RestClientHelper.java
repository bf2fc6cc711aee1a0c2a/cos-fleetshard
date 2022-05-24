package org.bf2.cos.fleetshard.sync.client;

import java.net.ConnectException;
import java.util.concurrent.Callable;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.bf2.cos.fleetshard.support.function.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClientHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientHelper.class);

    public static <T extends Throwable> void run(ThrowingRunnable<T> runnable) {
        try {
            runnable.run();
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            LOGGER.warn("error={}, status={}, message={}",
                response.hasEntity() ? response.readEntity(String.class) : "undefined",
                response.getStatus(),
                e.getMessage());

            throw new FleetManagerClientException(e, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new FleetManagerClientException(e.getMessage(), e);
            } else {
                throw new FleetManagerClientException(e);
            }
        } catch (Throwable e) {
            throw new FleetManagerClientException(e);
        }
    }

    public static <T> T call(Callable<T> callable) {
        try {
            return callable.call();
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            LOGGER.warn("error={}, status={}, message={}",
                response.hasEntity() ? response.readEntity(String.class) : "undefined",
                response.getStatus(),
                e.getMessage());

            throw new FleetManagerClientException(e, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new FleetManagerClientException(e.getMessage(), e);
            } else {
                throw new FleetManagerClientException(e);
            }
        } catch (Throwable e) {
            throw new FleetManagerClientException(e);
        }
    }
}
