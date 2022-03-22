package org.bf2.cos.fleetshard.sync.it.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class WireMockTestResource implements QuarkusTestResourceLifecycleManager {
    private com.github.tomakehurst.wiremock.WireMockServer server;

    private final Map<String, String> args = new HashMap<>();

    @Override
    public void init(Map<String, String> initArgs) {
        if (initArgs != null) {
            this.args.putAll(initArgs);
        }
    }

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

    protected Map<String, String> getArguments() {
        return Collections.unmodifiableMap(this.args);
    }

    protected void injectServerInstance(TestInjector testInjector) {
        testInjector.injectIntoFields(
            getServer(),
            new TestInjector.AnnotatedAndMatchesType(WireMockTestInstance.class,
                com.github.tomakehurst.wiremock.WireMockServer.class));
    }
}
