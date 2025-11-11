package no.fintlabs.core;

import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.AzureUserExternal;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreUser;

public class CoreUserMapper {
    public static CoreUser toDBUser(AzureUser azureUser) {
        return new CoreUser(HashKey.createHashKey(azureUser));
    }

    public static CoreUser toDBUser(AzureUserExternal azureUserExternal)  {
        return new CoreUser(HashKey.createHashKey(azureUserExternal));
    }

}
