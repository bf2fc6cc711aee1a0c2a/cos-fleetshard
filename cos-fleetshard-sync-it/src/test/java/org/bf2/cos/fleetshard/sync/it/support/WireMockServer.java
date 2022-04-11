package org.bf2.cos.fleetshard.sync.it.support;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static org.bf2.cos.fleetshard.sync.it.support.SyncTestSupport.untilAsserted;

public class WireMockServer extends com.github.tomakehurst.wiremock.WireMockServer {
    public WireMockServer(Options options) {
        super(options);
    }

    public void stubMatching(RequestMethod method, String pattern, Consumer<ResponseDefinitionBuilder> consumer) {
        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher);

        ResponseDefinitionBuilder response = WireMock.aResponse();
        consumer.accept(response);

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        ContentPattern<?> content,
        Consumer<ResponseDefinitionBuilder> consumer) {

        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher).withRequestBody(content);

        ResponseDefinitionBuilder response = WireMock.aResponse();
        consumer.accept(response);

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        Consumer<MappingBuilder> mappingConsumer,
        Consumer<ResponseDefinitionBuilder> consumer) {

        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher);

        mappingConsumer.accept(request);

        ResponseDefinitionBuilder response = WireMock.aResponse();
        consumer.accept(response);

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        String jsonPath,
        Consumer<ResponseDefinitionBuilder> consumer) {

        stubMatching(method, pattern, matchingJsonPath(jsonPath), consumer);
    }

    public void stubMatching(RequestMethod method, String pattern, Supplier<ResponseDefinitionBuilder> supplier) {
        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher);
        ResponseDefinitionBuilder response = supplier.get();

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        ContentPattern<?> content,
        Supplier<ResponseDefinitionBuilder> supplier) {

        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher).withRequestBody(content);
        ResponseDefinitionBuilder response = supplier.get();

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        String jsonPath,
        Supplier<ResponseDefinitionBuilder> supplier) {

        stubMatching(method, pattern, matchingJsonPath(jsonPath), supplier);
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        StringValuePattern bodyPatter,
        Consumer<ResponseDefinitionBuilder> consumer) {

        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher).withRequestBody(bodyPatter);

        ResponseDefinitionBuilder response = WireMock.aResponse();
        consumer.accept(response);

        stubFor(request.willReturn(response));
    }

    public void stubMatching(
        RequestMethod method,
        String pattern,
        StringValuePattern bodyPatter,
        Supplier<ResponseDefinitionBuilder> supplier) {

        UrlPathPattern matcher = WireMock.urlPathMatching(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher).withRequestBody(bodyPatter);
        ResponseDefinitionBuilder response = supplier.get();

        stubFor(request.willReturn(response));
    }

    public void stubPathEquals(RequestMethod method, String pattern, Consumer<ResponseDefinitionBuilder> consumer) {
        UrlPathPattern matcher = WireMock.urlPathEqualTo(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher);

        ResponseDefinitionBuilder response = WireMock.aResponse();
        consumer.accept(response);

        stubFor(request.willReturn(response));
    }

    public void stubPathEquals(RequestMethod method, String pattern, Supplier<ResponseDefinitionBuilder> supplier) {
        UrlPathPattern matcher = WireMock.urlPathEqualTo(pattern);
        MappingBuilder request = WireMock.request(method.getName(), matcher);
        ResponseDefinitionBuilder response = supplier.get();

        stubFor(request.willReturn(response));
    }

    public void until(RequestPatternBuilder requestPatternBuilder) {
        untilAsserted(() -> verify(requestPatternBuilder));
    }

    public void until(int count, RequestPatternBuilder requestPatternBuilder) {
        untilAsserted(() -> verify(0, requestPatternBuilder));
    }
}
