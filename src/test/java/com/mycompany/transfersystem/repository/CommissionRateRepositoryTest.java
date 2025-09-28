package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class CommissionRateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommissionRateRepository commissionRateRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    public void testFindByBranchAndCommissionScope() {
        // Create test branches
        Branch mainAdminBranch = new Branch();
        mainAdminBranch.setName("MAIN_ADMIN_BRANCH");
        entityManager.persistAndFlush(mainAdminBranch);

        Branch branchA = new Branch();
        branchA.setName("BRANCH_A");
        entityManager.persistAndFlush(branchA);

        // Create commission rates
        CommissionRate platformBaseFee = new CommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"));
        entityManager.persistAndFlush(platformBaseFee);

        CommissionRate branchASendingFee = new CommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"));
        entityManager.persistAndFlush(branchASendingFee);

        // Test finding platform base fee
        var foundPlatformFee = commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE);
        assertThat(foundPlatformFee).isPresent();
        assertThat(foundPlatformFee.get().getRateValue()).isEqualByComparingTo("1.50");
        assertThat(foundPlatformFee.get().getCommissionScope()).isEqualTo(CommissionScope.PLATFORM_BASE_FEE);

        // Test finding branch A sending fee
        var foundBranchFee = commissionRateRepository.findByBranchAndCommissionScope(branchA, CommissionScope.SENDING_BRANCH_FEE);
        assertThat(foundBranchFee).isPresent();
        assertThat(foundBranchFee.get().getRateValue()).isEqualByComparingTo("1.50");
        assertThat(foundBranchFee.get().getCommissionScope()).isEqualTo(CommissionScope.SENDING_BRANCH_FEE);

        // Test finding non-existent rate
        var notFound = commissionRateRepository.findByBranchAndCommissionScope(branchA, CommissionScope.PLATFORM_BASE_FEE);
        assertThat(notFound).isEmpty();
    }

    @Test
    public void testFindByBranch_IdAndCommissionScope() {
        // Create test branch
        Branch branchA = new Branch();
        branchA.setName("BRANCH_A");
        entityManager.persistAndFlush(branchA);

        // Create commission rate
        CommissionRate branchAReceivingFee = new CommissionRate(branchA, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"));
        entityManager.persistAndFlush(branchAReceivingFee);

        // Test finding by branch ID
        var found = commissionRateRepository.findByBranch_IdAndCommissionScope(branchA.getId(), CommissionScope.RECEIVING_BRANCH_FEE);
        assertThat(found).isPresent();
        assertThat(found.get().getRateValue()).isEqualByComparingTo("4.00");
        assertThat(found.get().getCommissionScope()).isEqualTo(CommissionScope.RECEIVING_BRANCH_FEE);
    }

    @Test
    public void testFindByBranch() {
        // Create test branch
        Branch branchA = new Branch();
        branchA.setName("BRANCH_A");
        entityManager.persistAndFlush(branchA);

        // Create multiple commission rates for the same branch
        CommissionRate sendingFee = new CommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"));
        entityManager.persistAndFlush(sendingFee);

        CommissionRate receivingFee = new CommissionRate(branchA, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"));
        entityManager.persistAndFlush(receivingFee);

        // Test finding all rates for branch
        var foundRates = commissionRateRepository.findByBranch(branchA);
        assertThat(foundRates).hasSize(2);
        assertThat(foundRates).extracting(CommissionRate::getCommissionScope)
                .containsExactlyInAnyOrder(CommissionScope.SENDING_BRANCH_FEE, CommissionScope.RECEIVING_BRANCH_FEE);
    }

    @Test
    public void testFindByCommissionScope() {
        // Create test branches
        Branch mainAdminBranch = new Branch();
        mainAdminBranch.setName("MAIN_ADMIN_BRANCH");
        entityManager.persistAndFlush(mainAdminBranch);

        Branch branchA = new Branch();
        branchA.setName("BRANCH_A");
        entityManager.persistAndFlush(branchA);

        // Create commission rates with same scope but different branches
        CommissionRate platformBaseFee1 = new CommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"));
        entityManager.persistAndFlush(platformBaseFee1);

        // Test finding all rates by scope
        var foundRates = commissionRateRepository.findByCommissionScope(CommissionScope.PLATFORM_BASE_FEE);
        assertThat(foundRates).hasSize(1);
        assertThat(foundRates.get(0).getCommissionScope()).isEqualTo(CommissionScope.PLATFORM_BASE_FEE);
    }
}
