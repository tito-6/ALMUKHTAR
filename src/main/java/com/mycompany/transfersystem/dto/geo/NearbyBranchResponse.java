package com.mycompany.transfersystem.dto.geo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NearbyBranchResponse {

    private Long branchId;
    private String name;
    private String address;
    private String city;
    private String country;
    private String phone;
    private BigDecimal distanceKm;
    private Boolean isOpenNow;
    private List<String> services;
}
