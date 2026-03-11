package com.mycompany.transfersystem.service.feezone;

import com.mycompany.transfersystem.entity.FeeZone;
import com.mycompany.transfersystem.repository.FeeZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeeZoneService {

    private final FeeZoneRepository feeZoneRepository;

    public FeeZoneService(FeeZoneRepository feeZoneRepository) {
        this.feeZoneRepository = feeZoneRepository;
    }

    public List<FeeZone> findAll() {
        return feeZoneRepository.findAll();
    }
}
