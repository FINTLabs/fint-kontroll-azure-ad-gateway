package no.fintlabs.core;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.HashKey;
import no.fintlabs.config.ConfigGroup;
import no.fintlabs.core.entity.CoreGroup;
import no.fintlabs.group.MsGraphGroupMapper;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
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

    @AllArgsConstructor
    static class CompareGroup implements Serializable {
        private long id;
        private String name;
    }

    static public CoreGroup toCoreGroup(CompareGroup compareGroup) {
        try {
            return new CoreGroup(
                    new HashKey(
                            SHA_256.get().digest(
                                    SerializationUtils.serialize(compareGroup)
                            )
                    ),
                    compareGroup.name
            );
        } catch (Exception e) {
            return null;
        }
    }

    static public CoreGroup toCoreGroup(@NotNull ResourceGroup resourceGroup, ConfigGroup configGroup) {
        long id = 0;
        try {
            id = Long.parseLong(resourceGroup.getId());
        } catch (NumberFormatException e) {
            log.error("Wrong ID gotten from kafka: '" + resourceGroup.getId() + "'");
        }

        CompareGroup compareGroup = new CompareGroup(
                id,
                MsGraphGroupMapper.getDisplayname(resourceGroup, configGroup)
        );
        return toCoreGroup(compareGroup);
    }

    static public CoreGroup toCoreGroup(@NotNull AzureGroup azureGroup) {
        CompareGroup compareGroup = new CompareGroup(
                azureGroup.getResourceGroupID(),
                azureGroup.getDisplayName()
        );
        return toCoreGroup(compareGroup);
    }
}