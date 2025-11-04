package no.fintlabs.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class DBMembership extends DBObject{
    private UUID id;
    private DBUser user;
    private DBGroup group;
}