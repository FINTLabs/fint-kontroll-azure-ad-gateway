package no.fintlabs.db;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.kafka.ResourceGroup;
import org.apache.commons.lang3.SerializationUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Setter
@Getter
@RequiredArgsConstructor
public class DBGroupMapper {
    static public DBGroup toDBGroup(AzureGroup azureGroup) throws NoSuchAlgorithmException {
        return new DBGroup(
                new HashKey(
                        MessageDigest
                                .getInstance("SHA-256")
                                .digest(
                                        SerializationUtils.serialize(azureGroup)
                                )
                )
        );
    }

    static public DBGroup toDBGroup(ResourceGroup resourceGroup) throws NoSuchAlgorithmException {
        return new DBGroup(
                new HashKey(
                        MessageDigest
                                .getInstance("SHA-256")
                                .digest(
                                        SerializationUtils.serialize(resourceGroup)
                                )
                )
        );
    }

}