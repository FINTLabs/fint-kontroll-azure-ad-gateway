package no.fintlabs.db.entity;
import lombok.*;
import no.fintlabs.azure.HashKey;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBUser extends DBObject {
    private HashKey hash;
}