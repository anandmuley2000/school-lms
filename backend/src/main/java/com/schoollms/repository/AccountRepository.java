package com.schoollms.repository;

import com.schoollms.domain.Account;
import com.schoollms.domain.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);

  Optional<Account> findByEmailIgnoreCase(String email);

  long countByRole(Role role);
}
