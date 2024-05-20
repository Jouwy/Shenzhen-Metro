package com.sustech.cs307.project2.ChineseSubwaySystem.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BusExitInfoDto {
    private String stationName;

    @NotEmpty(message = "The exit gate is required.")
    private String exitGate;

    @NotEmpty(message = "The bus name is required.")
    private String busName;

    @NotEmpty(message = "The bus line is required.")
    private String busLine;
}
