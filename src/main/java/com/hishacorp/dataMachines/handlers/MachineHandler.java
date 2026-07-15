package com.hishacorp.dataMachines.handlers;

import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.core.Machine;

public interface MachineHandler<T extends MachineType> {
    /**
     * Handles the tick logic for the machine.
     * 
     * @param machine The machine instance.
     */
    void tick(Machine machine);
}