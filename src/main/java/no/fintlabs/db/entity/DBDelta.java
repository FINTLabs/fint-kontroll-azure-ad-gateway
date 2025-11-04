package no.fintlabs.db.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.fintlabs.db.entity.DBObject;

@Setter
@Getter
@RequiredArgsConstructor
public class DBDelta extends DBObject {
    private String URL;
}