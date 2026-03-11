package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
