package org.bf2.cos.fleetshard.sync.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class FleetManagerRequestLoggingFilter implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(FleetManagerRequestLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext context) {
        LOG.debugf("Request %s %s to address %s",
            context.getMethod(),
            context.getUri().getPath(),
            context.getUri().getAuthority());
    }
}
