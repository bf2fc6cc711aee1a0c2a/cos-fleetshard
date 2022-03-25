package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class OidcTestResource extends WireMockTestResource {
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CLIENT_SECRET = UUID.randomUUID().toString();
    private static final String REALM = "rhoas";
    private static final String TOKEN_BODY = "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}";
    private static final String TOKEN_URL = "/auth/realms/" + REALM + "/protocol/openid-connect/token";

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(wireMockConfig());
        server.start();

        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=client_credentials"))
            .willReturn(WireMock
                .aResponse()
                .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        server.stubFor(WireMock.post(TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
            .willReturn(WireMock
                .aResponse()
                .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                .withBody(TOKEN_BODY)));

        return Map.of(
            "mas-sso-base-url", server.baseUrl(),
            "mas-sso-realm", REALM,
            "client-id", CLIENT_ID,
            "client-secret", CLIENT_SECRET);
    }

    protected WireMockConfiguration wireMockConfig() {
        return WireMockConfiguration.wireMockConfig()
            .dynamicPort()
            .notifier(new Slf4jNotifier(false))
            .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER);
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
