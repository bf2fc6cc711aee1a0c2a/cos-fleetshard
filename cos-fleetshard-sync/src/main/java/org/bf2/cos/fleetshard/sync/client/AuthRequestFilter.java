package org.bf2.cos.fleetshard.sync.client;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.bf2.cos.fleetshard.sync.metrics.MetricsID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.common.runtime.OidcConstants;

@ApplicationScoped
public class AuthRequestFilter implements ClientRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OidcClientRequestFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";
    private static final String METRICS_INVOKE = "connectors.oidc.invoke";

    @Inject
    AuthTokenSupplier tokenSupplier;
    @Inject
    @MetricsID(METRICS_INVOKE)
    MetricsRecorder recorderInvoke;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        this.recorderInvoke.record(
            () -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_SCHEME_WITH_SPACE + tokenSupplier.get()),
            e -> {
                if (e instanceof DisabledOidcClientException) {
                    LOGGER.warn(
                        "OidcClient is not enabled, aborting the request with HTTP 500 error: {}",
                        e.getMessage());

                    requestContext.abortWith(Response.status(500).build());
                } else {
                    LOGGER.warn(
                        "Access token is not available, aborting the request with HTTP 401 error: {}",
                        e.getMessage());

                    requestContext.abortWith(Response.status(401).build());
                }

                tokenSupplier.reset();
            });
    }
}
