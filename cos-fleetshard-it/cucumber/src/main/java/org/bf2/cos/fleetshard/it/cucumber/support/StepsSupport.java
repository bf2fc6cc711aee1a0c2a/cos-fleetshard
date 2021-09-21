package org.bf2.cos.fleetshard.it.cucumber.support;

import javax.inject.Inject;

import org.bf2.cos.fleetshard.it.cucumber.Awaiter;
import org.bf2.cos.fleetshard.it.cucumber.ConnectorContext;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import io.fabric8.kubernetes.client.KubernetesClient;

public class StepsSupport {
    public static final ParseContext PARSER = JsonPath.using(
        Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build());

    @Inject
    protected KubernetesClient kubernetesClient;
    @Inject
    protected Awaiter awaiter;
    @Inject
    protected ConnectorContext ctx;

    protected StepsSupport() {
    }
}
