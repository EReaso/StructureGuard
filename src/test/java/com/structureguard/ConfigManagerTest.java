package com.structureguard;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigManager}, focusing on the blocked-blocks
 * ore-protection feature added in this PR.
 */
class ConfigManagerTest {

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
     * Build a fresh {@link ConfigManager} from a programmatically-created
     * {@link YamlConfiguration}, bypassing the real config file on disk.
     */
    private ConfigManager configManagerFrom(YamlConfiguration yaml) {
        // Copy all non-section values into the plugin config so ConfigManager reads them
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                plugin.getConfig().set(key, yaml.get(key));
            }
        }
        return new ConfigManager(plugin);
    }

    // -----------------------------------------------------------------------
    // blocked-blocks loading
    // -----------------------------------------------------------------------

    @Test
    void testBlockedBlocksLoadedFromConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-blocks", Arrays.asList("ANCIENT_DEBRIS", "DIAMOND_ORE"));

        ConfigManager manager = configManagerFrom(yaml);

        assertTrue(manager.getBlockedBlocks().contains("ANCIENT_DEBRIS"),
                "ANCIENT_DEBRIS should be in the blocked list");
        assertTrue(manager.getBlockedBlocks().contains("DIAMOND_ORE"),
                "DIAMOND_ORE should be in the blocked list");
        assertEquals(2, manager.getBlockedBlocks().size());
    }

    @Test
    void testCaseInsensitiveMaterialNames() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-blocks", Arrays.asList("ancient_debris"));

        ConfigManager manager = configManagerFrom(yaml);

        assertTrue(manager.getBlockedBlocks().contains("ANCIENT_DEBRIS"),
                "Material names should be normalised to uppercase");
    }

    @Test
    void testInvalidMaterialIsRejectedAndNotLoaded() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-blocks", Arrays.asList("NOT_A_REAL_BLOCK_99999"));

        ConfigManager manager = configManagerFrom(yaml);

        assertTrue(manager.getBlockedBlocks().isEmpty(),
                "Invalid material names should not be added to the blocked list");
    }

    @Test
    void testMixedValidAndInvalidMaterials() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-blocks", Arrays.asList("ANCIENT_DEBRIS", "FAKE_BLOCK_XYZ"));

        ConfigManager manager = configManagerFrom(yaml);

        assertTrue(manager.getBlockedBlocks().contains("ANCIENT_DEBRIS"),
                "Valid material should be accepted");
        assertFalse(manager.getBlockedBlocks().contains("FAKE_BLOCK_XYZ"),
                "Invalid material should be rejected");
        assertEquals(1, manager.getBlockedBlocks().size());
    }

    @Test
    void testEmptyBlockedBlocksWhenNotConfigured() {
        YamlConfiguration yaml = new YamlConfiguration();
        // blocked-blocks not set at all

        ConfigManager manager = configManagerFrom(yaml);

        assertTrue(manager.getBlockedBlocks().isEmpty(),
                "Missing blocked-blocks should yield an empty set");
    }

    // -----------------------------------------------------------------------
    // blocked-block-message
    // -----------------------------------------------------------------------

    @Test
    void testCustomBlockedBlockMessage() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-block-message", "&aCustom deny message!");

        ConfigManager manager = configManagerFrom(yaml);

        assertEquals("&aCustom deny message!", manager.getBlockedBlockMessage());
    }

    @Test
    void testDefaultBlockedBlockMessageUsedWhenNotSet() {
        YamlConfiguration yaml = new YamlConfiguration();
        // blocked-block-message not set

        ConfigManager manager = configManagerFrom(yaml);

        assertEquals("&cYou cannot break this block here!", manager.getBlockedBlockMessage(),
                "Default message should be used when key is absent");
    }

    // -----------------------------------------------------------------------
    // Immutability guarantee
    // -----------------------------------------------------------------------

    @Test
    void testGetBlockedBlocksReturnsUnmodifiableSet() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocked-blocks", Arrays.asList("ANCIENT_DEBRIS"));

        ConfigManager manager = configManagerFrom(yaml);

        assertThrows(UnsupportedOperationException.class,
                () -> manager.getBlockedBlocks().add("DIAMOND_ORE"),
                "getBlockedBlocks() should return an unmodifiable view");
    }
}
