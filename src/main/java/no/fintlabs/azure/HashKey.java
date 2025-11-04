package no.fintlabs.azure;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public class HashKey {
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
}