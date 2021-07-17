package org.bf2.cos.fleetshard.operator.it.support;

import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class OidcSetup implements QuarkusTestResourceLifecycleManager {
    private static String CLIENT_ID = UUID.randomUUID().toString();
    private static String CLIENT_SECRET = UUID.randomUUID().toString();
    private static String REALM = "rhoas";
    private static String TOKEN_BODY = "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}";
    private static String TOKEN_URL = "/auth/realms/" + REALM + "/protocol/openid-connect/token";

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(wireMockConfig().dynamicPort().useChunkedTransferEncoding(ChunkedEncodingPolicy.NEVER));
        server.start();

        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                matching("grant_type=client_credentials"))
            .willReturn(WireMock
                .aResponse()
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
            .willReturn(WireMock
                .aResponse()
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        return Map.of(
            "quarkus.oidc-client-filter.register-filter", "false",
            "quarkus.oidc-client.client-enabled", "true",
            "quarkus.oidc-client.auth-server-url", server.baseUrl() + "/auth/realms/" + REALM,
            "quarkus.oidc-client.token-path", "/protocol/openid-connect/token",
            "quarkus.oidc-client.discovery-enabled", "false",
            "quarkus.oidc-client.client-id", CLIENT_ID,
            "quarkus.oidc-client.credentials.secret", CLIENT_SECRET);
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
