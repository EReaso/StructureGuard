package com.structureguard;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;

/**
 * Prevents players from breaking blocks listed in the {@code blocked-blocks}
 * configuration section (e.g., ANCIENT_DEBRIS).
 *
 * <p>This protection is enforced globally, regardless of WorldGuard regions.
 * Players with the {@code structureguard.bypass.oreprotection} permission are
 * exempt from this restriction.</p>
 */
public class OreProtectionListener implements Listener {

    private final StructureGuardPlugin plugin;

    public OreProtectionListener(StructureGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Set<String> blockedBlocks = plugin.getConfigManager().getBlockedBlocks();
        if (blockedBlocks.isEmpty()) {
            return;
        }

        String materialName = event.getBlock().getType().name();
        if (!blockedBlocks.contains(materialName)) {
            return;
        }

        Player player = event.getPlayer();

        // Allow operators / players with bypass permission to break blocked blocks
        if (player.hasPermission("structureguard.bypass.oreprotection")) {
            return;
        }

        event.setCancelled(true);
        String message = plugin.getConfigManager().getBlockedBlockMessage();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        plugin.getConfigManager().debug("Blocked " + player.getName()
                + " from breaking " + materialName
                + " at " + event.getBlock().getLocation());
    }
}
