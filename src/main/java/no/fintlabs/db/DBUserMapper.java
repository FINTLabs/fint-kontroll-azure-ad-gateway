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
    public static DBUser toDBUser(AzureUser azureUser) throws NoSuchAlgorithmException {
        return new DBUser(
                new HashKey(
                        MessageDigest
                                .getInstance("SHA-256")
                                .digest(
                                        SerializationUtils.serialize(azureUser)
                                )
                )
        );
    }

    public static DBUser toDBUser(AzureUserExternal azureUserExternal) throws NoSuchAlgorithmException {
        return new DBUser(MessageDigest.getInstance("SHA-256").digest(SerializationUtils.serialize(azureUserExternal)));
    }
    /*private static byte[] encodeDeterministic(AzureUserExternal u) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
        try (DataOutputStream out = new DataOutputStream(bos)) {
            // Fixed field order. Normalize where it matters.
            writeUuid(out, u.getId());                         // required GUID
            writeUuid(out, u.getIdpUserObjectId());            // optional GUID

            writeStr(out, u.getMail(), true);                  // lowercase emails
            writeStr(out, u.getUserPrincipalName(), true);     // lowercase UPN
            writeStr(out, u.getEmployeeId(), false);
            writeStr(out, u.getStudentId(), false);

            writeTriBool(out, u.getAccountEnabled());          // nullable bool
        }
        return bos.toByteArray();
    }

    private static byte[] encodeDeterministic(AzureUser u) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
        try (DataOutputStream out = new DataOutputStream(bos)) {
            // Fixed field order. Normalize where it matters.
            writeUuid(out, u.getId());                         // required GUID
            writeUuid(out, u.getIdpUserObjectId());            // optional GUID

            writeStr(out, u.getMail(), true);                  // lowercase emails
            writeStr(out, u.getUserPrincipalName(), true);     // lowercase UPN
            writeStr(out, u.getEmployeeId(), false);
            writeStr(out, u.getStudentId(), false);

            writeTriBool(out, u.getAccountEnabled());          // nullable bool
        }
        return bos.toByteArray();
    }*/

}
