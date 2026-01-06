package no.fintlabs.core;

import no.fintlabs.TestUtils;
import no.fintlabs.azure.HashKey;
import no.fintlabs.core.entity.CoreObject;
import no.fintlabs.core.entity.CoreUser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CoreSinkTest {

    @Test
    void MakeSureSinkWaitsIfFull() {
        CoreSink sink = new CoreSink();
        sink.persist(UUID.randomUUID().toString(), TestUtils.getRandomUser(), CoreObjectEventType.CREATED);

    }

    @Test
    void persist() {
    }

    @Test
    void testPersist() {
    }

    @Test
    void flush() {
    }

    @Test
    void getEnabled() {
    }

    @Test
    void getSink() {
    }

    @Test
    void setEnabled() {
    }
}