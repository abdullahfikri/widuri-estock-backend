package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findFirstByToken(String token);

    boolean existsByEmail(String email);

    boolean existsByAddressesId(int addressesId);
}
