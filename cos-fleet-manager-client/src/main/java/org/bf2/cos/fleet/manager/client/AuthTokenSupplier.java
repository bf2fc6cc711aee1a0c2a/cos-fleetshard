package org.bf2.cos.fleet.manager.client;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.bf2.cos.fleetshard.support.exceptions.WrappedRuntimeException;
import org.bf2.cos.fleetshard.support.metrics.MetricsID;
import org.bf2.cos.fleetshard.support.metrics.MetricsRecorder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import io.quarkus.oidc.client.runtime.TokensHelper;

@ApplicationScoped
public class AuthTokenSupplier implements Supplier<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OidcClientRequestFilter.class);
    private static final String METRICS_REFRESH = "connectors.oidc.refresh";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";

    @Inject
    OidcClients clients;
    @Inject
    ClientConfig config;

    @Inject
    @MetricsID(METRICS_REFRESH)
    MetricsRecorder recorder;

    @ConfigProperty(name = "client-id")
    String clientId;
    @ConfigProperty(name = "client-secret")
    String clientSecret;

    private TokensHelper tokensHelper;
    private OidcClientSupplier supplier;

    @PostConstruct
    public void setUp() {
        this.tokensHelper = new TokensHelper();
        if (config.manager().ssoUri().isPresent()) {
            this.supplier = new OidcStaticClientSupplier();
            LOGGER.info("Using static Oidc client.");
        } else {
            this.supplier = new OidcDiscoveryClientSupplier();
            LOGGER.info("Using discovery Oidc client.");
        }
    }

    @Override
    public String get() {
        return RestClientHelper.call(() -> {
            OidcClient client = client();
            if (client == null) {
                reset();
                throw new IllegalStateException("Unable to create oidc client");
            }

            return this.tokensHelper.getTokens(client)
                .await().atMost(config.manager().ssoTimeout())
                .getAccessToken();
        });
    }

    public void reset() {
        this.supplier.close();
    }

    public OidcClient client() {
        return recorder.recordCallable(() -> {
            return this.supplier.get();
        }, e -> {
            LOGGER.info("Error creating OidcClient: {}", e.getMessage());
            this.supplier.close();

            throw WrappedRuntimeException.launderThrowable(e);
        });
    }

    private String getAuthServerUrl() {
        try {
            return getAuthClient().getSsoProviders().requiredAt("/valid_issuer").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AuthApi getAuthClient() {
        final URI ssoUriBase = config.manager().ssoProviderUri();
        final URI ssoUri = UriBuilder.fromUri(ssoUriBase).path("/api/kafkas_mgmt/v1").build();

        return RestClientBuilder.newBuilder()
            .baseUri(ssoUri)
            .connectTimeout(config.manager().connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(config.manager().readTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .build(AuthApi.class);
    }

    private interface OidcClientSupplier extends Supplier<OidcClient>, AutoCloseable {
        @Override
        void close();
    }

    private class OidcStaticClientSupplier implements OidcClientSupplier {
        private final URI ssoUri;

        private volatile boolean initialized;
        private volatile OidcClient value;

        OidcStaticClientSupplier() {
            this.value = null;
            this.initialized = false;
            this.ssoUri = config.manager().ssoUri().orElseThrow(() -> new IllegalArgumentException("Missing SSO URI"));
        }

        @Override
        public OidcClient get() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        OidcClient t = create();

                        if (value != null) {
                            IOUtils.closeQuietly(value);
                        }

                        value = t;
                        initialized = true;
                    }
                }
            }

            return value;
        }

        @Override
        public void close() {
            initialized = false;
        }

        private OidcClient create() {
            OidcClientConfig cfg = new OidcClientConfig();
            cfg.setId(config.cluster().id());
            cfg.setAuthServerUrl(ssoUri.toString());
            cfg.setTokenPath(TOKEN_PATH);
            cfg.setDiscoveryEnabled(false);
            cfg.setClientId(clientId);
            cfg.getCredentials().setSecret(clientSecret);

            LOGGER.debug("Creating OidcClient for cluster with id {}. AuthServer ({}), TokenPath ({}).",
                config.cluster().id(), cfg.getAuthServerUrl(), cfg.getTokenPath());

            return clients.newClient(cfg).await().atMost(config.manager().ssoTimeout());
        }
    }

    private class OidcDiscoveryClientSupplier implements OidcClientSupplier {
        private final long durationNanos;

        private volatile OidcClient value;
        private volatile long expirationNanos;

        OidcDiscoveryClientSupplier() {
            this.durationNanos = config.manager().ssoProviderRefreshTimeout().toNanos();
            this.expirationNanos = 0;
            this.value = null;
        }

        @Override
        public OidcClient get() {
            long exp = expirationNanos;
            long now = System.nanoTime();

            if (exp == 0 || now - exp >= 0) {
                synchronized (this) {
                    if (exp == expirationNanos) {
                        OidcClient t = create();

                        if (value != null) {
                            IOUtils.closeQuietly(value);
                        }

                        if (t != null) {
                            exp = durationNanos == 0 ? Long.MAX_VALUE : now + durationNanos;
                        } else {
                            exp = 0;
                        }

                        value = t;
                        expirationNanos = exp;
                    }
                }
            }

            return value;
        }

        @Override
        public void close() {
            expirationNanos = 0;
        }

        private OidcClient create() {
            OidcClientConfig cfg = new OidcClientConfig();
            cfg.setId(config.cluster().id());
            cfg.setAuthServerUrl(getAuthServerUrl());
            cfg.setTokenPath(TOKEN_PATH);
            cfg.setDiscoveryEnabled(false);
            cfg.setClientId(clientId);
            cfg.getCredentials().setSecret(clientSecret);

            LOGGER.info("Discovered auth server url: {}", cfg.getAuthServerUrl());

            return clients.newClient(cfg).await().atMost(config.manager().ssoTimeout());
        }
    }
}
