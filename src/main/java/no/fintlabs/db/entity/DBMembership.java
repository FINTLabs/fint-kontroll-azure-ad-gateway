package no.fintlabs.db.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.azure.HashKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBMembership extends DBObject implements Serializable {
    @Serial
    private HashKey hash;
    @Serial
    private DBUser user;
    @Serial
    private DBGroup group;
}