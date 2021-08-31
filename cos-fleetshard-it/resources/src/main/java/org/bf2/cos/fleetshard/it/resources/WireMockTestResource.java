package org.bf2.cos.fleetshard.it.resources;

import java.util.Map;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class WireMockTestResource implements QuarkusTestResourceLifecycleManager {
    private com.github.tomakehurst.wiremock.WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new com.github.tomakehurst.wiremock.WireMockServer(wireMockConfig());
        server.start();

        return doStart(server);
    }

    protected WireMockConfiguration wireMockConfig() {
        return WireMockConfiguration.wireMockConfig()
            .dynamicPort()
            .notifier(new Slf4jNotifier(false))
            .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER);
    }

    protected abstract Map<String, String> doStart(com.github.tomakehurst.wiremock.WireMockServer server);

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    protected com.github.tomakehurst.wiremock.WireMockServer getServer() {
        return server;
    }

    protected void injectServerInstance(TestInjector testInjector) {
        testInjector.injectIntoFields(
            getServer(),
            new TestInjector.AnnotatedAndMatchesType(WireMockTestInstance.class,
                com.github.tomakehurst.wiremock.WireMockServer.class));
    }
}
