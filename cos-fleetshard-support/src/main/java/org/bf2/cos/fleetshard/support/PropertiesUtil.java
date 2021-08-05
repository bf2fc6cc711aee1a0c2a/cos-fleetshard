package org.bf2.cos.fleetshard.support;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

public final class PropertiesUtil {
    private PropertiesUtil() {
    }

    @SuppressWarnings("unchecked")
    public static byte[] asBytes(Properties prop) {
        return asBytes((Map) prop);
    }

    public static String asBytesBase64(Properties props) {
        return Base64.getEncoder().encodeToString(asBytes(props));
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
}
