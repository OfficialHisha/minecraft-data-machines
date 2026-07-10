package com.hishacorp.dataMachines.listeners;

import com.hishacorp.dataMachines.core.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.hishacorp.dataMachines.core.MachineManager.KEY_UUID;

public class ChunkListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(ChunkListener.class);
    private final MachineManager machineManager;
    private final JavaPlugin plugin;

    private final Set<Long> scannedChunks = new HashSet<>();

    public ChunkListener(JavaPlugin plugin, MachineManager machineManager) {
        this.plugin = plugin;
        this.machineManager = machineManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        log.debug("onChunkLoad");

        Chunk chunk = e.getChunk();
        long key = chunkKey(chunk);

        // already scanned (this chunk can load multiple times depending on conditions)
        if (!scannedChunks.add(key)) return;

        // Scan 1 tick later to ensure entities are fully present/settled
        Bukkit.getScheduler().runTask(plugin, () -> scanChunkEntities(chunk));
    }

    private void scanChunkEntities(Chunk chunk) {
        Location center = new Location(chunk.getWorld(), chunk.getX() * 16 + 8, 128, chunk.getZ() * 16 + 8);
        double radius = 8.5;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 256, radius)) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            if (container.has(KEY_UUID, PersistentDataType.STRING)) {
                log.debug("Found a machine entity {}", entity.getEntityId());
                machineManager.loadMachineFromContainer(entity, container);
            }
        }
    }

    private long chunkKey(Chunk chunk) {
        // pack chunk coords into one long
        return (((long) chunk.getX()) << 32) ^ (chunk.getZ() & 0xffffffffL);
    }
}