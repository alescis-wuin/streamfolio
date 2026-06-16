package dev.sey.streamfolio.repository;

import dev.sey.streamfolio.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserAccount> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserAccount> findById(Long id);
}
