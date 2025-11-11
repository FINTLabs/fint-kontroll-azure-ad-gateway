package no.fintlabs.db.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import java.sql.Timestamp;
import java.util.UUID;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObject {
    private Timestamp timestamp;
}