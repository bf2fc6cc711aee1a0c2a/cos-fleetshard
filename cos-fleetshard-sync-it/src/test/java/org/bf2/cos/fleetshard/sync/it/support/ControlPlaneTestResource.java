package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public abstract class ControlPlaneTestResource implements QuarkusTestResourceLifecycleManager {
    private FleetManagerMockServer fleetManager;
    private OidcMockServer oidc;

    private final Map<String, String> args = new HashMap<>();

    @Override
    public void init(Map<String, String> initArgs) {
        if (initArgs != null) {
            this.args.putAll(initArgs);
        }
    }

    @Override
    public Map<String, String> start() {

        oidc = new OidcMockServer(wireMockConfig());
        oidc.start();

        configure(oidc);

        fleetManager = new FleetManagerMockServer(wireMockConfig());
        fleetManager.start();

        fleetManager.stubMatching(
            RequestMethod.GET,
            "/api/kafkas_mgmt/v1/sso_providers",
            resp -> {
                ObjectNode body = new ObjectMapper().createObjectNode();
                body.put("valid_issuer", oidc.baseUrl() + "/auth/realms/" + OidcMockServer.OIDC_REALM);

                resp.withHeader(ContentTypeHeader.KEY, APPLICATION_JSON)
                    .withJsonBody(body);
            });

        configure(fleetManager);

        return Map.of(
            "control-plane-base-url", fleetManager.baseUrl(),
            "mas-sso-base-url", oidc.baseUrl(),
            "mas-sso-realm", OidcMockServer.OIDC_REALM,
            "client-id", OidcMockServer.OIDC_CLIENT_ID,
            "client-secret", OidcMockServer.OIDC_CLIENT_SECRET);
    }

    protected WireMockConfiguration wireMockConfig() {
        return WireMockConfiguration.wireMockConfig()
            .dynamicPort()
            .notifier(new Slf4jNotifier(false))
            .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER);
    }

    protected void configure(FleetManagerMockServer server) {
    }

    protected void configure(OidcMockServer server) {
    }

    @Override
    public synchronized void stop() {
        try {
            if (fleetManager != null) {
                fleetManager.stop();
                fleetManager = null;
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (oidc != null) {
                oidc.stop();
                oidc = null;
            }
        } catch (Exception e) {
            // ignored
        }
    }

    protected WireMockServer getFleetManager() {
        return fleetManager;
    }

    protected WireMockServer getOidc() {
        return oidc;
    }

    protected Map<String, String> getArguments() {
        return Collections.unmodifiableMap(this.args);
    }

    @Override
    public void inject(QuarkusTestResourceLifecycleManager.TestInjector testInjector) {
        testInjector.injectIntoFields(
            getFleetManager(),
            new TestInjector.AnnotatedAndMatchesType(
                FleetManagerTestInstance.class,
                FleetManagerMockServer.class));

        testInjector.injectIntoFields(
            getOidc(),
            new TestInjector.AnnotatedAndMatchesType(
                OidcTestInstance.class,
                OidcMockServer.class));
    }
}
