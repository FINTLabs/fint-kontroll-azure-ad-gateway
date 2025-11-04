package no.fintlabs.db.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import java.sql.Timestamp;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObject extends Object {
    private Timestamp timestamp;
}