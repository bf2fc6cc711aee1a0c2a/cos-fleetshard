package org.bf2.cos.fleetshard.sync.it.support;

import com.github.tomakehurst.wiremock.core.Options;

public class FleetManagerMockServer extends WireMockServer {
    public FleetManagerMockServer(Options options) {
        super(options);
    }
}
