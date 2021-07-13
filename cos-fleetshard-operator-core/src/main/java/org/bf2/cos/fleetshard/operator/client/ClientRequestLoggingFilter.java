package org.bf2.cos.fleetshard.operator.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class ClientRequestLoggingFilter implements ClientRequestFilter {
    private static final Logger LOG = Logger.getLogger(ClientRequestLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext context) {
        LOG.debugf("Request %s %s to address %s",
            context.getMethod(),
            context.getUri().getPath(),
            context.getUri().getAuthority());
    }
}
