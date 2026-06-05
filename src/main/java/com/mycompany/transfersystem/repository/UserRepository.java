package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByFundId(Long fundId);
    Optional<User> findByPhone(String phone);

    List<User> findByRole(UserRole role);

    List<User> findByBranch_IdAndRole(Long branchId, UserRole role);
}