package org.bf2.cos.fleetshard.operator.client;

import java.net.ConnectException;
import java.util.concurrent.Callable;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleetshard.support.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FleetManagerClientHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClientHelper.class);

    public static <T extends Throwable> void run(ThrowingRunnable<T> runnable) {
        try {
            runnable.run();
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();
            final Error error = response.readEntity(Error.class);

            LOGGER.warn("code={}, reason={}, status={}, message={}",
                error.getCode(),
                error.getReason(),
                response.getStatus(),
                e.getMessage());

            throw new FleetManagerClientException(e, error, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new FleetManagerClientException(e.getMessage());
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
            final Error error = response.readEntity(Error.class);

            LOGGER.warn("code={}, reason={}, status={}, message={}",
                error.getCode(),
                error.getReason(),
                response.getStatus(),
                e.getMessage());

            throw new FleetManagerClientException(e, error, response.getStatus());
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                LOGGER.warn("{}", e.getMessage());
                throw new FleetManagerClientException(e.getMessage());
            } else {
                throw new FleetManagerClientException(e);
            }
        } catch (Throwable e) {
            throw new FleetManagerClientException(e);
        }
    }
}
