package no.fintlabs.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.azure.HashKey;

@Setter
@Getter
@AllArgsConstructor
public class CoreDevice extends CoreObject {
    private HashKey hash;
}
