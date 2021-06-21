package org.bf2.cos.fleetshard.operator.client;

import java.net.ConnectException;
import java.util.concurrent.Callable;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.bf2.cos.fleet.manager.api.model.cp.Error;
import org.bf2.cos.fleetshard.operator.support.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FleetManagerClientHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetManagerClientHelper.class);

    public static <T extends Throwable> void run(ThrowingRunnable<T> runnable) {
        try {
            runnable.run();
        } catch (WebApplicationException e) {
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new FleetManagerClientException(e);
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
            LOGGER.warn("{}", e.getResponse().readEntity(Error.class).getReason(), e);
            throw new FleetManagerClientException(e);
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
