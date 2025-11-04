package no.fintlabs.db;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBUser extends DBObject {
    private UUID id;
}