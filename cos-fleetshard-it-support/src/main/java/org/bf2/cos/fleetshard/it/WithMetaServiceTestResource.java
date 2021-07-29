package org.bf2.cos.fleetshard.it;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(MetaServiceTestResource.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithMetaServiceTestResource {
    /**
     * Image to use.
     */
    String image();

    /**
     * Prefix used for properties.
     */
    String prefix();

    /**
     * Port to use, defaults to 8080.
     */
    int port() default 8080;
}
