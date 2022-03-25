package org.bf2.cos.fleetshard.sync.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class FleetManagerResponseLoggingFilter implements ClientResponseFilter {
    private static final Logger LOG = Logger.getLogger(FleetManagerResponseLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        LOG.debugf("Response to %s %s to address %s, status %d",
            requestContext.getMethod(),
            requestContext.getUri().getPath(),
            requestContext.getUri().getAuthority(),
            responseContext.getStatus());
    }
}
