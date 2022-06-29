package org.bf2.cos.fleet.manager.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class RestClientResponseLoggingFilter implements ClientResponseFilter {
    private static final Logger LOG = Logger.getLogger(RestClientResponseLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        LOG.debugf("Response to %s %s to address %s, status %d",
            requestContext.getMethod(),
            requestContext.getUri().getPath(),
            requestContext.getUri().getAuthority(),
            responseContext.getStatus());
    }
}
