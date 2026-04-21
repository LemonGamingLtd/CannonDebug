/*
 * This file is part of CannonProfiler, licensed under the MIT License (MIT).
 *
 * Copyright (c) Origin <http://www.originmc.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.originmc.cannondebug.cmd;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.originmc.cannondebug.CannonDebugPlugin;
import org.originmc.cannondebug.utils.NumberUtils;

import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.YELLOW;

public final class CmdRegion extends CommandExecutor {

    public CmdRegion(CannonDebugPlugin plugin, CommandSender sender, String[] args, String permission) {
        super(plugin, sender, args, permission);
    }

    @Override
    public boolean perform() {
        Player player = (Player) sender;
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));

        Region selection;
        com.sk89q.worldedit.world.World selectionWorld = session.getSelectionWorld();
        try {
            if (selectionWorld == null) {
                throw new IncompleteRegionException();
            }
            selection = session.getSelection(selectionWorld);
        } catch (IncompleteRegionException e) {
            sender.sendMessage(RED + "You must make a WorldEdit selection first!");
            return true;
        }

        // Do nothing if selection is not a cuboid.
        if (!(selection instanceof CuboidRegion)) {
            sender.sendMessage(RED + "Region selected must be a cuboid!");
            return true;
        }

        // Do nothing if selection is too large.
        int maxArea = NumberUtils.getNumericalPerm(sender, "cannondebug.maxarea.", 500);
        if (selection.getVolume() > maxArea) {
            sender.sendMessage(String.format(RED + "Region selected is too large! " + GRAY + "(Max area = %s blocks)", maxArea));
            return true;
        }

        // Handle selection for all blocks within this region.
        org.bukkit.World world = BukkitAdapter.adapt(selectionWorld);
        BlockVector3 max = selection.getMaximumPoint();
        BlockVector3 min = selection.getMinimumPoint();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    this.plugin.handleSelection(user, world.getBlockAt(x, y, z));
                }
            }
        }

        // Send complete message.
        sender.sendMessage(YELLOW + "All possible selections have been toggled.");
        return true;
    }

}
