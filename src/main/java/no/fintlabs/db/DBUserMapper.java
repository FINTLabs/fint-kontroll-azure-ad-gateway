package no.fintlabs.db;

import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.AzureUserExternal;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.entity.DBUser;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class DBUserMapper {
    public static DBUser toDBUser(AzureUser azureUser) {
        return new DBUser(HashKey.createHashKey(azureUser));
    }

    public static DBUser toDBUser(AzureUserExternal azureUserExternal)  {
        return new DBUser(HashKey.createHashKey(azureUserExternal));
    }

}
