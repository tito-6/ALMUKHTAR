package com.mycompany.transfersystem.service.family;

import com.mycompany.transfersystem.dto.family.CreateFamilyGroupRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.FamilySpendingLimitExceededException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyWalletService {

    private final FamilyGroupRepository groupRepository;
    private final FamilyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuditService auditService;

    @Transactional
    public FamilyGroup createGroup(Long ownerId, CreateFamilyGroupRequest dto) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));
        FamilyGroup group = FamilyGroup.builder()
                .ownerUser(owner).name(dto.getName())
                .monthlySpendingLimit(dto.getMonthlySpendingLimit())
                .currency(dto.getCurrency()).build();
        group = groupRepository.save(group);
        memberRepository.save(FamilyMember.builder()
                .familyGroup(group).memberUser(owner)
                .role(FamilyMember.FamilyRole.OWNER).build());
        auditService.log("FAMILY_GROUP_CREATED", "FAMILY_GROUP", group.getId(), null, owner);
        return group;
    }

    @Transactional
    public void addMember(Long groupId, Long ownerId, Long memberUserId, BigDecimal memberLimit) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Family group not found: " + groupId));
        if (!group.getOwnerUser().getId().equals(ownerId)) {
            throw new ConditionNotMetException("Only group owner can add members");
        }
        User member = userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + memberUserId));
        memberRepository.save(FamilyMember.builder()
                .familyGroup(group).memberUser(member)
                .role(FamilyMember.FamilyRole.MEMBER)
                .monthlySpendingLimit(memberLimit).build());
        auditService.log("FAMILY_MEMBER_ADDED", "FAMILY_GROUP", groupId, "member=" + memberUserId, group.getOwnerUser());
    }

    @Transactional
    public void removeMember(Long groupId, Long ownerId, Long memberUserId) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Family group not found: " + groupId));
        if (!group.getOwnerUser().getId().equals(ownerId)) {
            throw new ConditionNotMetException("Only group owner can remove members");
        }
        FamilyMember fm = memberRepository.findByFamilyGroupIdAndMemberUserId(groupId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in group"));
        fm.setActive(false);
        memberRepository.save(fm);
        auditService.log("FAMILY_MEMBER_REMOVED", "FAMILY_GROUP", groupId, "member=" + memberUserId, group.getOwnerUser());
    }

    public boolean isActiveMember(Long userId) {
        return !memberRepository.findByMemberUserIdAndActiveTrue(userId).isEmpty();
    }

    @Transactional(readOnly = true)
    public void checkSpendingLimit(Long userId, BigDecimal amount, String currency) {
        List<FamilyMember> memberships = memberRepository.findByMemberUserIdAndActiveTrue(userId);
        for (FamilyMember fm : memberships) {
            FamilyGroup group = fm.getFamilyGroup();
            if (!group.getCurrency().equals(currency)) continue;
            BigDecimal limit = fm.getMonthlySpendingLimit() != null ? fm.getMonthlySpendingLimit() : group.getMonthlySpendingLimit();
            if (limit == null) continue;
            BigDecimal monthlySpend = calculateMonthlySpend(userId, currency);
            if (monthlySpend.add(amount).compareTo(limit) > 0) {
                throw new FamilySpendingLimitExceededException(
                        "Monthly spending limit exceeded: spent=" + monthlySpend + " + " + amount + " > limit=" + limit);
            }
        }
    }

    private BigDecimal calculateMonthlySpend(Long userId, String currency) {
        Wallet wallet = walletRepository.findByUser_Id(userId).orElse(null);
        if (wallet == null) return BigDecimal.ZERO;
        YearMonth ym = YearMonth.now();
        Instant start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return walletTransactionRepository.sumDebitsByWalletAndCurrencyBetween(
                wallet.getId(), currency, start, end);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatement(Long groupId, Long requesterId, YearMonth month) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Family group not found: " + groupId));
        Map<String, Object> statement = new HashMap<>();
        statement.put("groupId", groupId);
        statement.put("groupName", group.getName());
        statement.put("month", month.toString());
        List<FamilyMember> members = memberRepository.findByFamilyGroupId(groupId);
        List<Map<String, Object>> memberStats = new ArrayList<>();
        for (FamilyMember fm : members) {
            Map<String, Object> ms = new HashMap<>();
            ms.put("userId", fm.getMemberUser().getId());
            ms.put("role", fm.getRole());
            ms.put("spent", calculateMonthlySpendForMonth(fm.getMemberUser().getId(), group.getCurrency(), month));
            ms.put("limit", fm.getMonthlySpendingLimit());
            memberStats.add(ms);
        }
        statement.put("members", memberStats);
        return statement;
    }

    private BigDecimal calculateMonthlySpendForMonth(Long userId, String currency, YearMonth month) {
        Wallet wallet = walletRepository.findByUser_Id(userId).orElse(null);
        if (wallet == null) return BigDecimal.ZERO;
        Instant start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return walletTransactionRepository.sumDebitsByWalletAndCurrencyBetween(wallet.getId(), currency, start, end);
    }
}
