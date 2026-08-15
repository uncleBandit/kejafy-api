package com.pitahmzalendo.kejafy.user.repository;


import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRole(UserRole role);

    List<User> findByActiveTrue();

    List<User> findByRoleAndActiveTrue(UserRole role);
}