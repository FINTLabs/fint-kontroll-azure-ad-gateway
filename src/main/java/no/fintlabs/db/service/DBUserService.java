package no.fintlabs.db.service;

import lombok.RequiredArgsConstructor;
import no.fintlabs.db.DBUserMapper;
import no.fintlabs.db.entity.DBUser;
import no.fintlabs.db.repository.DBUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DBUserService {

    private final DBUserRepository dbUserRepository;

    public DBUser getUserById(UUID id){
        return dbUserRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No user with id " + id));
    }

    public DBUser saveUser(no.fintlabs.azure.AzureUser user){
        return dbUserRepository.save(DBUserMapper.toDBUser(user));
    }

}
