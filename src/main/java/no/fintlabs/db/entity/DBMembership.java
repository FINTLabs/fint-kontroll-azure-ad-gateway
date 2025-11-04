package no.fintlabs.db.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBMembership extends DBObject {
    private UUID id;
    private DBUser user;
    private DBGroup group;
}