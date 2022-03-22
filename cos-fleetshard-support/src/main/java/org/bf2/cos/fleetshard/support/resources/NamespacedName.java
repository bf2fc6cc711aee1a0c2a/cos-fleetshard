package org.bf2.cos.fleetshard.support.resources;

import java.io.Serializable;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class NamespacedName implements Serializable, Comparable<NamespacedName> {
    private final String namespace;
    private final String name;

    public NamespacedName(String namespace, String name) {
        this.namespace = Objects.requireNonNull(namespace);
        this.name = Objects.requireNonNull(name);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamespacedName)) {
            return false;
        }

        NamespacedName id = (NamespacedName) o;
        return Objects.equals(getNamespace(), id.getNamespace())
            && Objects.equals(getName(), id.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespace(), getName());
    }

    @Override
    public String toString() {
        return "NamespacedName{" +
            "namespace='" + namespace + '\'' +
            ", name='" + name + '\'' +
            '}';
    }

    @Override
    public int compareTo(NamespacedName other) {
        int answer = this.namespace.compareTo(other.namespace);
        if (answer == 0) {
            answer = this.name.compareTo(other.name);
        }

        return answer;
    }

    public static NamespacedName of(HasMetadata resource) {
        return new NamespacedName(
            resource.getMetadata().getName(),
            resource.getMetadata().getNamespace());
    }
}
