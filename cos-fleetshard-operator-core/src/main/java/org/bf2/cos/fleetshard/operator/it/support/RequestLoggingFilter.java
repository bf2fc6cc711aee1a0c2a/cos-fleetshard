package org.bf2.cos.fleetshard.operator.it.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);

    @Context
    UriInfo info;
    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) {
        LOG.debugf("method: %s, path: %s, content: %s",
            context.getMethod(),
            info.getPath(),
            readEntityStream(context));
    }

    private String readEntityStream(ContainerRequestContext context) {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final InputStream inputStream = context.getEntityStream();

        try {
            IOUtils.copy(inputStream, outStream);
            context.setEntityStream(new ByteArrayInputStream(outStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outStream.toString(StandardCharsets.UTF_8);
    }
}
