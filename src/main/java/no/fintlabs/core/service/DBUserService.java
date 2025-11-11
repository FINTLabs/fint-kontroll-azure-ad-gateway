package no.fintlabs.core.service;

import lombok.RequiredArgsConstructor;
import no.fintlabs.core.CoreUserMapper;
import no.fintlabs.core.entity.CoreUser;
import no.fintlabs.core.repository.DBUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DBUserService {

    private final DBUserRepository dbUserRepository;

    public CoreUser getUserById(UUID id){
        return dbUserRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No user with id " + id));
    }

    public CoreUser saveUser(no.fintlabs.azure.AzureUser user){
        return dbUserRepository.save(CoreUserMapper.toDBUser(user));
    }

}
