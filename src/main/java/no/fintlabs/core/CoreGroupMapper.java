package no.fintlabs.core;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.SerializationUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Log4j2
@Setter
@Getter
@RequiredArgsConstructor
public class CoreGroupMapper {

    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    });

    static public CoreGroup toDBGroup(AzureGroup azureGroup) {
        return new CoreGroup(
                new HashKey(
                        SHA_256.get().digest(
                                SerializationUtils.serialize(azureGroup)
                        )
                )
        );
    }

    static public CoreGroup toDBGroup(ResourceGroup resourceGroup) {
        try {
            return new CoreGroup(
                    new HashKey(
                        SHA_256.get().digest(
                            SerializationUtils.serialize(resourceGroup)
                        )
                    )
            );
        } catch (Exception e) {
            return null;
        }
    }

}