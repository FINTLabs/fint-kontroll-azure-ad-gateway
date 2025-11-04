package no.fintlabs.db;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.azure.AzureGroup;
import no.fintlabs.db.entity.DBGroup;
import no.fintlabs.kafka.ResourceGroup;

import java.util.UUID;

@Setter
@Getter
@RequiredArgsConstructor
public class DBGroupMapper {
    static public DBGroup toDBGroup(AzureGroup group) {
        return new DBGroup(UUID.fromString(group.getId()));
    }

    static public DBGroup toDBGroup(ResourceGroup group) {
        return new DBGroup(UUID.fromString(group.getId()));
    }

}