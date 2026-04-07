package com.structureguard;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OreProtectionListener}.
 *
 * <p>MockBukkit provides a real server environment (Material enums, player
 * stubs, etc.) while Mockito is used to mock the Block so we can control
 * which material is being broken without needing a full world.</p>
 */
class OreProtectionListenerTest {

    private ServerMock server;
    private StructureGuardPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(StructureGuardPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Build a listener whose ConfigManager is a Mockito mock that returns
     * {@code blockedBlocks} and a fixed deny message.
     */
    private OreProtectionListener listenerWithBlockedSet(Set<String> blockedBlocks) {
        ConfigManager mockConfig = mock(ConfigManager.class);
        when(mockConfig.getBlockedBlocks()).thenReturn(blockedBlocks);
        when(mockConfig.getBlockedBlockMessage()).thenReturn("&cBlocked!");

        StructureGuardPlugin mockPlugin = mock(StructureGuardPlugin.class);
        when(mockPlugin.getConfigManager()).thenReturn(mockConfig);

        return new OreProtectionListener(mockPlugin);
    }

    /** Simulate breaking {@code material} as {@code player}. */
    private BlockBreakEvent simulateBreak(PlayerMock player, Material material) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.getLocation()).thenReturn(player.getLocation());
        return new BlockBreakEvent(block, player);
    }

    // -----------------------------------------------------------------------
    // Core protection logic
    // -----------------------------------------------------------------------

    @Test
    void testBlockedMaterialCancelsEvent() {
        PlayerMock player = server.addPlayer();
        OreProtectionListener listener = listenerWithBlockedSet(Set.of("ANCIENT_DEBRIS"));

        BlockBreakEvent event = simulateBreak(player, Material.ANCIENT_DEBRIS);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Breaking a blocked block should cancel the event");
    }

    @Test
    void testNonBlockedMaterialDoesNotCancelEvent() {
        PlayerMock player = server.addPlayer();
        OreProtectionListener listener = listenerWithBlockedSet(Set.of("ANCIENT_DEBRIS"));

        BlockBreakEvent event = simulateBreak(player, Material.DIRT);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Breaking an unblocked block should not cancel the event");
    }

    @Test
    void testEmptyBlockListAllowsAll() {
        PlayerMock player = server.addPlayer();
        OreProtectionListener listener = listenerWithBlockedSet(Collections.emptySet());

        BlockBreakEvent event = simulateBreak(player, Material.ANCIENT_DEBRIS);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Empty blocked-blocks list should allow all blocks");
    }

    // -----------------------------------------------------------------------
    // Bypass permission
    // -----------------------------------------------------------------------

    @Test
    void testBypassPermissionAllowsBreakingBlockedBlock() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "structureguard.bypass.oreprotection", true);

        OreProtectionListener listener = listenerWithBlockedSet(Set.of("ANCIENT_DEBRIS"));

        BlockBreakEvent event = simulateBreak(player, Material.ANCIENT_DEBRIS);
        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(),
                "Player with bypass permission should be allowed to break blocked blocks");
    }

    @Test
    void testWithoutBypassPermissionBlocksAreBlocked() {
        PlayerMock player = server.addPlayer();
        // explicitly no bypass permission

        OreProtectionListener listener = listenerWithBlockedSet(Set.of("DIAMOND_ORE"));

        BlockBreakEvent event = simulateBreak(player, Material.DIAMOND_ORE);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(),
                "Player without bypass permission should have the event cancelled");
    }

    // -----------------------------------------------------------------------
    // Player feedback
    // -----------------------------------------------------------------------

    @Test
    void testMessageSentToPlayerWhenBlocked() {
        PlayerMock player = server.addPlayer();
        OreProtectionListener listener = listenerWithBlockedSet(Set.of("ANCIENT_DEBRIS"));

        BlockBreakEvent event = simulateBreak(player, Material.ANCIENT_DEBRIS);
        listener.onBlockBreak(event);

        assertNotNull(player.nextComponentMessage(),
                "A deny message should be sent to the player when a blocked block is broken");
    }

    @Test
    void testNoMessageSentWhenBlockIsNotInList() {
        PlayerMock player = server.addPlayer();
        OreProtectionListener listener = listenerWithBlockedSet(Set.of("ANCIENT_DEBRIS"));

        BlockBreakEvent event = simulateBreak(player, Material.STONE);
        listener.onBlockBreak(event);

        assertNull(player.nextComponentMessage(),
                "No message should be sent when breaking an unblocked block");
    }
}
