package no.fintlabs.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.azure.HashKey;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
public class CoreMembership extends CoreObject implements Serializable {
    @Serial
    private HashKey hash;
    @Serial
    private CoreUser user;
    @Serial
    private CoreGroup group;
}