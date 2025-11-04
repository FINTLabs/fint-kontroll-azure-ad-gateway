package no.fintlabs.db;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.SerializationUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Log4j2
@Setter
@Getter
@RequiredArgsConstructor
public class DBGroupMapper {

    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    });

    static public DBGroup toDBGroup(AzureGroup azureGroup) {
        return new DBGroup(
                new HashKey(
                        SHA_256.get().digest(
                                SerializationUtils.serialize(azureGroup)
                        )
                )
        );
    }

    static public DBGroup toDBGroup(ResourceGroup resourceGroup) {
        try {
            return new DBGroup(
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