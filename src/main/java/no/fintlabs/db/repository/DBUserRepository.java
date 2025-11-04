package no.fintlabs.db.repository;

import no.fintlabs.db.entity.DBUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface DBUserRepository extends JpaRepository<DBUser, UUID> {
}
