package org.bf2.cos.fleetshard.sync.it.support;

import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class OidcMockServer extends WireMockServer {
    public static final String OIDC_CLIENT_ID = UUID.randomUUID().toString();
    public static final String OIDC_CLIENT_SECRET = UUID.randomUUID().toString();
    public static final String OIDC_REALM = "rhoas";
    public static final String OIDC_TOKEN_BODY = "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}";
    public static final String OIDC_TOKEN_URL = "/auth/realms/" + OIDC_REALM + "/protocol/openid-connect/token";

    public OidcMockServer(Options options) {
        super(options);
    }

    @Override
    public void start() {
        super.start();

        stubFor(WireMock.post(OIDC_TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=client_credentials"))
            .willReturn(WireMock
                .aResponse()
                .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                .withBody(OIDC_TOKEN_BODY)));

        stubFor(WireMock.post(OIDC_TOKEN_URL)
            .withRequestBody(
                WireMock.matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
            .willReturn(WireMock
                .aResponse()
                .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                .withBody(OIDC_TOKEN_BODY)));
    }
}
