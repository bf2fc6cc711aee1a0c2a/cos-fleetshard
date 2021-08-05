package org.bf2.cos.fleetshard.it;

import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class OidcTestResource extends WireMockTestResource {
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CLIENT_SECRET = UUID.randomUUID().toString();
    private static final String REALM = "rhoas";
    private static final String TOKEN_BODY = "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}";
    private static final String TOKEN_URL = "/auth/realms/" + REALM + "/protocol/openid-connect/token";

    @Override
    protected Map<String, String> doStart(WireMockServer server) {
        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=client_credentials"))
            .willReturn(WireMock
                .aResponse()
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
            .willReturn(WireMock
                .aResponse()
                .withHeader("Content-Type", APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        return Map.of(
            "mas-sso-base-url", server.baseUrl(),
            "mas-sso-realm", REALM,
            "client-id", CLIENT_ID,
            "client-secret", CLIENT_SECRET,
            "quarkus.oidc-client.token-path", "/protocol/openid-connect/token",
            "quarkus.oidc-client.discovery-enabled", "false");
    }
}
