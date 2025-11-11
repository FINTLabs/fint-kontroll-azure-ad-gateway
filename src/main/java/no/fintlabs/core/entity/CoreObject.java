package no.fintlabs.core.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import java.sql.Timestamp;

@Setter
@Getter
@RequiredArgsConstructor
public class CoreObject {
    private Timestamp timestamp;
}