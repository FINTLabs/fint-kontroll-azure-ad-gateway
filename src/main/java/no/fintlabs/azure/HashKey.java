package no.fintlabs.azure;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@AllArgsConstructor
public class HashKey {
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    });

    private final byte[] hash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HashKey)) return false;
        return Arrays.equals(hash, ((HashKey) o).hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }

    public static HashKey createHashKey(Serializable object) {
        return new HashKey(
                SHA_256.get().digest(
                    SerializationUtils.serialize(object)
                )
        );
    }
}