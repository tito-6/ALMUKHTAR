package com.mycompany.transfersystem.service.status;

import com.mycompany.transfersystem.dto.status.BranchPickupStatusDto;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.BranchIncident;
import com.mycompany.transfersystem.entity.SystemIncident;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.BranchIncidentRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.SystemIncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationalStatusService {

    private final BranchRepository branchRepository;
    private final BranchIncidentRepository branchIncidentRepository;
    private final SystemIncidentRepository systemIncidentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> publicSummary() {
        List<SystemIncident> global = systemIncidentRepository.findByActiveTrue();
        List<BranchPickupStatusDto> branches = branchRepository.findAll().stream()
                .map(this::toPickupDto)
                .toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("systemIncidents", global.stream().map(this::incidentMap).toList());
        m.put("branches", branches);
        return m;
    }

    @Transactional(readOnly = true)
    public BranchPickupStatusDto branchPickup(Long branchId) {
        Branch b = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        return toPickupDto(b);
    }

    private Map<String, Object> incidentMap(SystemIncident i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("component", i.getComponent().name());
        m.put("severity", i.getSeverity().name());
        m.put("title", i.getPublicTitle());
        m.put("message", i.getPublicMessage());
        return m;
    }

    private BranchPickupStatusDto toPickupDto(Branch b) {
        List<BranchIncident> active = branchIncidentRepository.findByBranch_IdAndActiveTrue(b.getId());
        boolean cashShort = active.stream().anyMatch(BranchIncident::isCashShortagePublic);
        boolean outage = active.stream().anyMatch(BranchIncident::isBranchOutage);
        boolean integration = active.stream().anyMatch(BranchIncident::isIntegrationFailure);
        boolean wa = active.stream().anyMatch(BranchIncident::isWhatsappDegraded);
        String msg = active.stream()
                .map(BranchIncident::getPublicMessage)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse(null);
        boolean pickup = !cashShort && !outage;
        return BranchPickupStatusDto.builder()
                .branchId(b.getId())
                .branchName(b.getName())
                .city(b.getCity())
                .pickupPossible(pickup)
                .publicMessage(msg)
                .cashShortageFlag(cashShort)
                .branchOutage(outage)
                .integrationDegraded(integration)
                .whatsappDegradedAtBranch(wa)
                .build();
    }
}
