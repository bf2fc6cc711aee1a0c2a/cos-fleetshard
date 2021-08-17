package org.bf2.cos.fleetshard.support;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(K key, V value, Object... keyVals) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);

        for (int i = 0; i < keyVals.length; i += 2) {
            map.put(
                (K) keyVals[i],
                (V) keyVals[i + 1]);
        }

        return map;
    }
}
