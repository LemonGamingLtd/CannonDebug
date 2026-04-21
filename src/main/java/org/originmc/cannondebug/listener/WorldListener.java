package org.originmc.cannondebug.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.originmc.cannondebug.BlockSelection;
import org.originmc.cannondebug.CannonDebugPlugin;
import org.originmc.cannondebug.EntityTracker;
import org.originmc.cannondebug.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.originmc.cannondebug.utils.MaterialUtils.isDispenser;
import static org.originmc.cannondebug.utils.MaterialUtils.isExplosives;
import static org.originmc.cannondebug.utils.MaterialUtils.isStacker;

public class WorldListener implements Listener {

    private static final long MAX_PENDING_TICKS = 2;

    private static final double TNT_MATCH_RADIUS_SQUARED = 2.25D;

    private final CannonDebugPlugin plugin;

    private final List<PendingDispense> pendingDispenses = new ArrayList<>();

    public WorldListener(CannonDebugPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void queueDispenseTracking(BlockDispenseEvent event) {
        // Do nothing if block is not a dispenser.
        Block block = event.getBlock();
        if (!isDispenser(block.getType())) return;

        // Do nothing if not shot TNT.
        if (!isExplosives(event.getItem().getType())) return;

        // Do nothing if dispenser has no direction to trace from.
        if (!(block.getBlockData() instanceof Directional directional)) return;

        // Gather every selection currently watching this dispenser.
        List<BlockSelection> selections = getSelections(block.getLocation());
        if (selections.isEmpty()) {
            return;
        }

        // Match the TNT entity when it spawns so vanilla Paper dispense behavior stays intact.
        prunePendingDispenses();
        pendingDispenses.add(new PendingDispense(getExpectedDispenseLocation(block, directional), plugin.getCurrentTick(), selections));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void startProfiling(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        PendingDispense pendingDispense = findPendingDispense(tnt.getLocation());
        if (pendingDispense == null) {
            return;
        }

        EntityTracker tracker = new EntityTracker(tnt.getType(), plugin.getCurrentTick());
        tracker.setEntity(tnt);
        plugin.getActiveTrackers().add(tracker);

        for (BlockSelection selection : pendingDispense.selections) {
            selection.setTracker(tracker);
        }

        pendingDispenses.remove(pendingDispense);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void startProfiling(EntityChangeBlockEvent event) {
        // Do nothing if the material is not used for stacking in cannons.
        Block block = event.getBlock();
        if (!isStacker(block.getType())) return;

        // Do nothing if block is not turning into a falling block.
        if (!(event.getEntity() instanceof FallingBlock)) return;

        // Loop through each user profile.
        BlockSelection selection;
        EntityTracker tracker = null;
        for (User user : plugin.getUsers().values()) {
            // Do nothing if user is not attempting to profile current block.
            selection = user.getSelection(block.getLocation());
            if (selection == null) {
                continue;
            }

            // Build a new tracker due to it being used.
            if (tracker == null) {
                tracker = new EntityTracker(event.getEntityType(), plugin.getCurrentTick());
                tracker.setEntity(event.getEntity());
                plugin.getActiveTrackers().add(tracker);
            }

            // Add block tracker to user.
            selection.setTracker(tracker);
        }
    }

    private List<BlockSelection> getSelections(Location location) {
        List<BlockSelection> selections = new ArrayList<>();
        for (User user : plugin.getUsers().values()) {
            BlockSelection selection = user.getSelection(location);
            if (selection != null) {
                selections.add(selection);
            }
        }
        return selections;
    }

    private Location getExpectedDispenseLocation(Block block, Directional directional) {
        return block.getLocation().clone()
                .add(0.5, 0.5, 0.5)
                .add(directional.getFacing().getDirection().multiply(0.7));
    }

    private PendingDispense findPendingDispense(Location entityLocation) {
        prunePendingDispenses();

        PendingDispense closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (PendingDispense pendingDispense : pendingDispenses) {
            if (!pendingDispense.expectedLocation.getWorld().equals(entityLocation.getWorld())) {
                continue;
            }

            double distance = pendingDispense.expectedLocation.distanceSquared(entityLocation);
            if (distance > TNT_MATCH_RADIUS_SQUARED || distance >= closestDistance) {
                continue;
            }

            closest = pendingDispense;
            closestDistance = distance;
        }
        return closest;
    }

    private void prunePendingDispenses() {
        long currentTick = plugin.getCurrentTick();
        Iterator<PendingDispense> iterator = pendingDispenses.iterator();
        while (iterator.hasNext()) {
            PendingDispense pendingDispense = iterator.next();
            if (currentTick - pendingDispense.tick > MAX_PENDING_TICKS) {
                iterator.remove();
            }
        }
    }

    private static final class PendingDispense {

        private final Location expectedLocation;

        private final long tick;

        private final List<BlockSelection> selections;

        private PendingDispense(Location expectedLocation, long tick, List<BlockSelection> selections) {
            this.expectedLocation = expectedLocation;
            this.tick = tick;
            this.selections = selections;
        }
    }

}
