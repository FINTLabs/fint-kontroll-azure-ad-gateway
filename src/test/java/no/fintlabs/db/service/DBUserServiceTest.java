package no.fintlabs.db.service;

import no.fintlabs.azure.AzureUser;
import no.fintlabs.azure.HashKey;
import no.fintlabs.db.DBUserMapper;
import no.fintlabs.db.entity.DBUser;
import no.fintlabs.db.repository.DBUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DBUserServiceTest {

    @Mock
    private DBUserRepository dbUserRepository;

    @InjectMocks
    private DBUserService buUserService;

    private UUID validUuid;
    private DBUser mockDBUser;
    private AzureUser mockAzureUser;

    @BeforeEach
    void setUp() {
        validUuid = UUID.randomUUID();
        mockDBUser = new DBUser(HashKey.createHashKey(UUID.randomUUID()));
        mockAzureUser = AzureUser.builder().idpUserObjectId(UUID.randomUUID().toString()).build();
    }

    @Test
    void getUserById_WithValidId_ReturnsUser() {
        when(dbUserRepository.findById(validUuid)).thenReturn(Optional.of(mockDBUser));

        DBUser result = buUserService.getUserById(validUuid);

        assertNotNull(result);
        assertEquals(mockDBUser, result);
    }

    @Test
    void getUserById_WithInvalidUUID_ThrowsIllegalArgumentException() {
        UUID invalidUuid = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> buUserService.getUserById(invalidUuid));
    }

    @Test
    void getUserById_WithNonExistentId_ThrowsIllegalArgumentException() {
        when(dbUserRepository.findById(validUuid)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> buUserService.getUserById(validUuid));
    }

    @Test
    void saveUser_WithValidUser_ReturnsSavedUser() {
        DBUser mappedUser = DBUserMapper.toDBUser(mockAzureUser);
        when(dbUserRepository.save(any(DBUser.class))).thenReturn(mappedUser);

        DBUser result = buUserService.saveUser(mockAzureUser);

        assertNotNull(result);
        assertEquals(mappedUser, result);
    }
}
