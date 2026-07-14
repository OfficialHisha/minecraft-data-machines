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
    double globalModifier,
    Map<String, Double> perTypeModifiers,
    Map<Integer, String> progressTextures,
    MachineProperties properties
) {
    public record MachineProperties(
        boolean alwaysDrop,
        boolean stopOnFullBuffer,
        boolean allowPushing,
        String redstoneRequired,
        boolean fuelRequired,
        List<String> fuelItems,
        String requiresPermission
    ) {}
}