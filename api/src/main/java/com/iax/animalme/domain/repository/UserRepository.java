package com.iax.animalme.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.enums.UserStatus;
import com.iax.animalme.domain.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
    List<User> findByEmailContainingIgnoreCase(String email);
    List<User> findByFirstNameContainingIgnoreCase(String firstName);
}
