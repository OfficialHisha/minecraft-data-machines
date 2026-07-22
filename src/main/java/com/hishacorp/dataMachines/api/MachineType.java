package com.hishacorp.dataMachines.api;

import java.util.List;
import java.util.Map;

public record MachineType(
    String id,
    String name,
    List<String> supportedRecipeTypes,
    List<Integer> inputSlots,
    List<Integer> outputSlots,
    List<Integer> fuelSlots,
    Integer progressSlot,
    Integer fuelProgressSlot,
    double globalModifier,
    Map<String, Double> perTypeModifiers,
    Map<Integer, String> progressTextures,
    Map<Integer, String> fuelProgressTextures,
    MachineProperties properties
) {
    public record MachineProperties(
        boolean alwaysDrop,
        boolean stopOnFullBuffer,
        boolean allowPushing,
        String redstoneRequired,
        boolean fuelRequired,
        Map<String, Integer> fuelItems,
        String requiresPermission
    ) {}
}