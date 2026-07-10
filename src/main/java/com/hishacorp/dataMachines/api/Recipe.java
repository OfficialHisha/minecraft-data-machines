package com.hishacorp.dataMachines.api;

import java.util.List;
import java.util.Map;

public record Recipe(
    String id,
    String type,
    List<ItemStackData> inputs,
    List<ItemStackData> outputs,
    int processingTime
) {
    public record ItemStackData(String item, int amount, double chance) {}
}