package no.fintlabs.db.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.azure.HashKey;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBMembership extends DBObject{
    private HashKey hash;
    private DBUser user;
    private DBGroup group;
}