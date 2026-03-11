package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CorporateAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorporateAccountRepository extends JpaRepository<CorporateAccount, Long> {
    Page<CorporateAccount> findByParentUser_Id(Long parentUserId, Pageable pageable);
}
