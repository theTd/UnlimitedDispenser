package org.totemcraft.unlimiteddispenser;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.Objects;

public class UnlimitedDispenserPlugin extends JavaPlugin implements Listener {
    private boolean configProtectBlock = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configProtectBlock = getConfig().getBoolean("protect-block", true);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean realDispense = false;

    @EventHandler
    void onDispense(final BlockDispenseEvent e) {
        if (realDispense) return;

        if (e.getBlock().getType() != Material.DISPENSER &&
                e.getBlock().getType() != Material.DROPPER) return;

        // check activate
        if (isActivatedDispenser(e.getBlock())) {
            // prepare real dispense
            e.setCancelled(true);

            Bukkit.getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    // save inv and fire real dispense
                    ItemStack[] inv = ((InventoryHolder) e.getBlock().getState()).getInventory().getContents();
                    final ItemStack[] copied = new ItemStack[inv.length];
                    for (int i = 0; i < inv.length; i++) {
                        copied[i] = inv[i];
                        if (copied[i] != null) {
                            copied[i] = copied[i].clone();
                        }
                    }

                    realDispense = true;
                    ((Dispenser) e.getBlock().getState()).dispense();
                    realDispense = false;

                    Bukkit.getScheduler().runTask(UnlimitedDispenserPlugin.this, new Runnable() {
                        @Override
                        public void run() {
                            ((InventoryHolder) e.getBlock().getState()).getInventory().setContents(copied);
                        }
                    });
                }
            });
        }
    }

    @EventHandler
    void access(SignChangeEvent e) {
        if (isValidSignContent(e.getLines()[0], e.getLines()[1], e.getLines()[2], e.getLines()[3])) {
            if (!e.getPlayer().hasPermission(Permissions.ACCESS + "")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    void protect(BlockBreakEvent e) {
        if (shouldProtectBlock(e.getBlock()) &&
                !e.getPlayer().hasPermission(Permissions.ACCESS + "")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    void protect(BlockExplodeEvent e) {
        if (!configProtectBlock) return;
        Iterator<Block> ite = e.blockList().iterator();
        while (ite.hasNext()) {
            Block b = ite.next();
            if (shouldProtectBlock(b)) {
                ite.remove();
            }
        }
    }

    @EventHandler
    void protect(EntityExplodeEvent e) {
        if (!configProtectBlock) return;
        Iterator<Block> ite = e.blockList().iterator();
        while (ite.hasNext()) {
            Block b = ite.next();
            if (shouldProtectBlock(b)) {
                ite.remove();
            }
        }
    }

    @EventHandler
    void protect(BlockBurnEvent e) {
        if (!configProtectBlock) return;
        if (shouldProtectBlock(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler
    void protect(BlockPistonExtendEvent e) {
        if (!configProtectBlock) return;
        for (Block block : e.getBlocks()) {
            if (shouldProtectBlock(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    void protect(BlockPistonRetractEvent e) {
        if (!configProtectBlock) return;
        for (Block block : e.getBlocks()) {
            if (shouldProtectBlock(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private boolean shouldProtectBlock(Block block) {
        if (isActivatedDispenser(block)) return true;
        if (isValidSign(block)) return true;
        Block up = block.getRelative(BlockFace.UP);
        if (up != null && up.getType() == Material.SIGN_POST && isValidSign(up)) return true;

        Block north = block.getRelative(BlockFace.NORTH);
        if (north.getType() == Material.WALL_SIGN && north.getData() == 2 && isValidSign(north)) {
            return true;
        }
        Block south = block.getRelative(BlockFace.SOUTH);
        if (south.getType() == Material.WALL_SIGN && south.getData() == 3 && isValidSign(south)) {
            return true;
        }
        Block west = block.getRelative(BlockFace.WEST);
        if (west.getType() == Material.WALL_SIGN && west.getData() == 4 && isValidSign(west)) {
            return true;
        }
        Block east = block.getRelative(BlockFace.EAST);
        if (east.getType() == Material.WALL_SIGN && east.getData() == 5 && isValidSign(east)) {
            return true;
        }

        return false;
    }

    private boolean isActivatedDispenser(Block block) {
        return (block.getType() == Material.DISPENSER ||
                block.getType() == Material.DROPPER) &&
                (isValidSign(block.getRelative(BlockFace.UP)) ||
                        isValidSign(block.getRelative(BlockFace.DOWN)) ||
                        isValidSign(block.getRelative(BlockFace.EAST)) ||
                        isValidSign(block.getRelative(BlockFace.SOUTH)) ||
                        isValidSign(block.getRelative(BlockFace.WEST)) ||
                        isValidSign(block.getRelative(BlockFace.NORTH)));
    }

    private boolean isValidSign(Block block) {
        if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
            Sign blockSign = (Sign) block.getState();
            String line1 = blockSign.getLine(0);
            String line2 = blockSign.getLine(1);
            String line3 = blockSign.getLine(2);
            String line4 = blockSign.getLine(3);
            return isValidSignContent(line1, line2, line3, line4);
        }
        return false;
    }

    private boolean isValidSignContent(String line1, String line2, String line3, String line4) {
        return (line1 == null || line1.trim().isEmpty()) &&
                Objects.equals("[Unlimited]", line2 == null ? null : line2.trim()) &&
                (line3 == null || line3.trim().isEmpty()) &&
                (line4 == null || line4.trim().isEmpty());
    }
}
