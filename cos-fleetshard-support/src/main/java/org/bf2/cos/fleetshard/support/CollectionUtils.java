package org.bf2.cos.fleetshard.support;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static byte[] asBytes(Map<String, String> props) {
        try (var os = new ByteArrayOutputStream()) {
            try (var bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.ISO_8859_1))) {
                for (var e : props.entrySet()) {
                    bw.write(e.getKey() + "=" + e.getValue());
                    bw.newLine();
                }

                bw.flush();
                return os.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String asBytesBase64(Map<String, String> props) {
        return Base64.getEncoder().encodeToString(asBytes(props));
    }

    /**
     * Build an unmodifiable map on top of a given map. Note tha thew given map is copied if not null.
     *
     * @param  map a map
     * @return     an unmodifiable map.
     */
    public static <K, V> Map<K, V> unmodifiableMap(Map<K, V> map) {
        return map == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Build a map from varargs.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(Supplier<Map<K, V>> creator, K key, V value, Object... keyVals) {
        Map<K, V> map = creator.get();
        map.put(key, value);

        for (int i = 0; i < keyVals.length; i += 2) {
            map.put(
                (K) keyVals[i],
                (V) keyVals[i + 1]);
        }

        return map;
    }

    /**
     * Build an immutable map from varargs.
     */
    public static <K, V> Map<K, V> immutableMapOf(Supplier<Map<K, V>> creator, K key, V value, Object... keyVals) {
        return Collections.unmodifiableMap(
            mapOf(creator, key, value, keyVals));
    }

    /**
     * Build a map from varargs.
     */
    public static <K, V> Map<K, V> mapOf(K key, V value, Object... keyVals) {
        return mapOf(HashMap::new, key, value, keyVals);
    }

    /**
     * Build an immutable map from varargs.
     */
    public static <K, V> Map<K, V> immutableMapOf(K key, V value, Object... keyVals) {
        return Collections.unmodifiableMap(
            mapOf(HashMap::new, key, value, keyVals));
    }
}
