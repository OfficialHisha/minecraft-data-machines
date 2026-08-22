package com.hishacorp.dataMachines.tasks;

import com.hishacorp.dataMachines.DataMachines;
import com.hishacorp.dataMachines.core.Machine;
import com.hishacorp.dataMachines.core.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

public class MachineHopperTask extends BukkitRunnable {
    private final MachineManager machineManager;

    public MachineHopperTask(MachineManager machineManager) {
        this.machineManager = machineManager;
    }

    @Override
    public void run() {
        for (Machine machine : machineManager.getActiveMachines()) {
            simulateHopperPush(machine);
        }
    }

    private void simulateHopperPush(Machine machine) {
        Location loc = machine.getLocation();
        
        // Check all 6 adjacent blocks for hoppers facing the machine
        // In Minecraft, a hopper pushes into the block it is facing.
        // To push INTO the machine, the hopper must be:
        // 1. Below the machine and facing UP
        // 2. Above the machine and facing DOWN
        // 3. Beside the machine and facing towards it
        
        // However, the user specifically mentioned "on top of, or on the side of machines"
        // Usually, hoppers BELOW push UP, hoppers ABOVE push DOWN, hoppers SIDE push SIDE.
        
        // We check all 6 directions. If the block is a hopper, we check its facing.
        // If it's facing the machine, we pull from it.
        
        checkAndPull(loc.clone().add(0, -1, 0), machine, BlockFace.UP); // Hopper below
        checkAndPull(loc.clone().add(0, 1, 0), machine, BlockFace.DOWN); // Hopper above
        checkAndPull(loc.clone().add(1, 0, 0), machine, BlockFace.WEST); // Hopper East
        checkAndPull(loc.clone().add(-1, 0, 0), machine, BlockFace.EAST); // Hopper West
        checkAndPull(loc.clone().add(0, 0, 1), machine, BlockFace.NORTH); // Hopper South
        checkAndPull(loc.clone().add(0, 0, -1), machine, BlockFace.SOUTH); // Hopper North
    }

    private void checkAndPull(Location hopperLoc, Machine machine, BlockFace facing) {
        Block block = hopperLoc.getBlock();
        if (block.getType() != Material.HOPPER) return;

        // Check if hopper is facing the machine using BlockData
        if (block.getBlockData() instanceof Directional directional) {
            if (directional.getFacing().equals(facing)) {
                BlockState state = block.getState();
                if (state instanceof Hopper blockHopper) {
                    pullFromHopper(blockHopper.getInventory(), machine);
                }
            }
        }
    }

    private void pullFromHopper(Inventory hopperInv, Machine machine) {
        ItemStack item = hopperInv.getItem(0);
        if (item == null || item.getType().isAir()) return;

        // Try to insert into fuel slots first
        Map<String, Integer> fuelItems = machine.getType().properties().fuelItems();
        List<Integer> fuelSlots = machine.getFuelSlots();
        
        if (isFuelItem(item, fuelItems)) {
            if (tryInsert(hopperInv, machine.getInventory(), item, fuelSlots)) {
                return;
            }
        }

        // Then try to insert into input slots
        List<Integer> inputSlots = machine.getInputSlots();
        if (tryInsert(hopperInv, machine.getInventory(), item, inputSlots)) {
            return;
        }
    }

    private boolean tryInsert(Inventory source, Inventory dest, ItemStack item, List<Integer> slots) {
        if (slots == null || slots.isEmpty()) return false;

        // The item to move is the one currently in the hopper's first slot
        ItemStack sourceStack = item;
        if (sourceStack == null || sourceStack.getType().isAir()) return false;

        int amountToMove = sourceStack.getAmount();
        int moved = 0;

        for (int slot : slots) {
            ItemStack destStack = dest.getItem(slot);
            int space = 0;
            if (destStack == null || destStack.getType().isAir()) {
                space = sourceStack.getMaxStackSize();
            } else if (destStack.isSimilar(sourceStack)) {
                space = destStack.getMaxStackSize() - destStack.getAmount();
            }

            if (space > 0) {
                int toAdd = Math.min(space, amountToMove - moved);
                if (destStack == null || destStack.getType().isAir()) {
                    ItemStack newStack = sourceStack.clone();
                    newStack.setAmount(toAdd);
                    dest.setItem(slot, newStack);
                } else {
                    destStack.setAmount(destStack.getAmount() + toAdd);
                }
                moved += toAdd;
            }
            if (moved >= amountToMove) break;
        }

        if (moved > 0) {
            ItemStack remaining = source.getItem(0);
            if (remaining != null) {
                if (remaining.getAmount() <= moved) {
                    source.setItem(0, null);
                } else {
                    remaining.setAmount(remaining.getAmount() - moved);
                }
            }
            return true;
        }

        return false;
    }

    private boolean isFuelItem(ItemStack item, Map<String, Integer> fuelItems) {
        if (fuelItems == null || fuelItems.isEmpty()) return false;
        for (String fuelId : fuelItems.keySet()) {
            if (DataMachines.isItemMatch(item, fuelId)) return true;
        }
        return false;
    }
}
