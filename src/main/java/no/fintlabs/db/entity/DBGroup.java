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
public class DBGroup extends DBObject {
    private HashKey kafkaChecksum;
    private HashKey entraChecksum;
    private String name;
}