package no.fintlabs.core.entity;
import lombok.*;
import no.fintlabs.azure.HashKey;

@Setter
@Getter
@AllArgsConstructor
public class CoreUser extends CoreObject {
    private HashKey hash;
}