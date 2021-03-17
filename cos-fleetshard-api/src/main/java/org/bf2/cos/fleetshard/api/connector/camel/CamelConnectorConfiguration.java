package org.bf2.cos.fleetshard.api.connector.camel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.camelk.v1alpha1.Kamelet;
import io.fabric8.camelk.v1alpha1.KameletBindingSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", refs = { @BuildableReference(Kamelet.class),
        @BuildableReference(KameletBindingSpec.class) })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CamelConnectorConfiguration {
    private List<Kamelet> kamelets;
    private KameletBindingSpec binding;

    public List<Kamelet> getKamelets() {
        return kamelets;
    }

    public void setKamelets(List<Kamelet> kamelets) {
        this.kamelets = kamelets;
    }

    public KameletBindingSpec getBinding() {
        return binding;
    }

    public void setBinding(KameletBindingSpec binding) {
        this.binding = binding;
    }

    @Override
    public String toString() {
        return "CamelConnectorConfiguration{" +
                "kamelets=" + kamelets +
                ", binding=" + binding +
                '}';
    }
}
