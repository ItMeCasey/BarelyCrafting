package com.itmecasey.barelycrafting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The one-time import of config/barelycrafting.properties, with the real files each released version wrote as fixtures. */
class ConfigMigrationTest {
    @TempDir
    Path dir;

    private static final String VERSION = "2026.11.4";
    private static final String[] RELEASES = {"v2026.11.1", "v2026.11.2", "v2026.11.3"};

    private Path old() {
        return dir.resolve(ConfigMigration.OLD_FILE);
    }

    private Path back() {
        return dir.resolve(ConfigMigration.OLD_FILE + ".back");
    }

    private byte[] fixture(String release, String name) throws IOException {
        try (var in = ConfigMigrationTest.class.getResourceAsStream("/config-fixtures/" + release + "/" + name + "/barelycrafting.properties")) {
            byte[] bytes = in.readAllBytes();
            Files.write(old(), bytes);
            return bytes;
        }
    }

    private Map<String, String> tomls() throws IOException {
        Map<String, String> all = new LinkedHashMap<>();
        for (String name : new String[] {ConfigMigration.COMMON_FILE, ConfigMigration.CLIENT_FILE}) {
            if (Files.exists(dir.resolve(name))) {
                all.putAll(ConfigMigration.readToml(dir.resolve(name)));
            }
        }
        return all;
    }

    private List<String> migrateText(String oldText, boolean client) throws IOException {
        Files.writeString(old(), oldText, StandardCharsets.UTF_8);
        return ConfigMigration.migrate(dir, client, VERSION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v2026.11.1", "v2026.11.2", "v2026.11.3"})
    void everyValueUserSetInAReleasedFileLandsInTheTomlAndTheOriginalMovesToABackup(String release) throws IOException {
        byte[] original = fixture(release, "edited");
        List<String> log = ConfigMigration.migrate(dir, true, VERSION);
        Map<String, String> all = tomls();
        assertEquals("false", all.get("buttons.rotate"));
        assertEquals("false", all.get("buttons.craft_all"));
        assertEquals("true", all.get("buttons.clear"));
        assertEquals("east,north", all.get("neighbors.faces"));
        assertEquals("3", all.get("neighbors.max"));
        assertEquals("false", all.get("neighbors.enabled"));
        assertEquals("100", all.get("craft.max_per_click"));
        assertEquals("modern", all.get("ui.theme"));
        assertFalse(Files.exists(old()), "the original is gone");
        byte[] backup = Files.readAllBytes(back());
        String header = new String(backup, StandardCharsets.UTF_8).lines().findFirst().orElseThrow();
        assertTrue(header.startsWith("# Migrated by Barely Crafting " + VERSION + " on "), header);
        assertTrue(header.contains("to config/barelycrafting-common.toml and config/barelycrafting-client.toml"), header);
        assertTrue(header.endsWith("; this file is no longer read. Kept as a backup."), header);
        byte[] headerBytes = (header + "\n").getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(original, Arrays.copyOfRange(backup, headerBytes.length, backup.length), "the exact original bytes follow the header");
        assertTrue(log.stream().anyMatch(l -> l.equals("Migrated neighbors.max = 3")), log.toString());
        assertEquals(1, log.stream().filter(l -> l.startsWith("Migrated config: ")).count(), log.toString());
        assertTrue(log.stream().noneMatch(l -> l.startsWith("Invalid") || l.startsWith("Ignored") || l.startsWith("Clamped") || l.startsWith("Could not")), log.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"v2026.11.1", "v2026.11.2", "v2026.11.3"})
    void aReleasedFileWithOnlyDefaultsImportsEveryKeyAtItsDefault(String release) throws IOException {
        fixture(release, "defaults");
        ConfigMigration.migrate(dir, true, VERSION);
        Map<String, String> all = tomls();
        for (Settings.Key key : Settings.KEYS) {
            assertEquals(key.fallback(), all.get(key.path()), key.path());
        }
        assertEquals(Settings.KEYS.size(), all.size());
        assertTrue(Files.exists(back()));
    }

    @Test
    void everyKeyOfTheOldFileIsAKeyOfTheNewConfig() throws IOException {
        for (String release : RELEASES) {
            fixture(release, "defaults");
            for (String line : Files.readAllLines(old())) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    assertNotNull(Settings.key(line.substring(0, line.indexOf('=')).trim()), release + " key " + line);
                }
            }
        }
    }

    @Test
    void aMissingOldFileCreatesNothing() throws IOException {
        assertTrue(ConfigMigration.migrate(dir, true, VERSION).isEmpty());
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void anEmptyOldFileCreatesNothingAndStaysPut() throws IOException {
        assertTrue(migrateText("", true).isEmpty());
        assertTrue(Files.exists(old()));
        assertFalse(Files.exists(back()));
        assertTrue(tomls().isEmpty());
    }

    @Test
    void aCorruptOldFileStaysWhereItIsAndGetsNoBackup() throws IOException {
        Files.writeString(old(), "buttons.rotate=false\nbroken=\\u12zz\n");
        List<String> log = ConfigMigration.migrate(dir, true, VERSION);
        assertTrue(log.get(0).startsWith("Could not read the old settings file"), log.toString());
        assertTrue(Files.exists(old()));
        assertFalse(Files.exists(back()));
        assertTrue(tomls().isEmpty());
        byte[] binary = {(byte) 0xff, (byte) 0xfe, 0, 1, 2, '=', 3, (byte) 0xc3, 0x28};
        Files.write(old(), binary);
        assertTrue(ConfigMigration.migrate(dir, true, VERSION).get(0).startsWith("Could not read the old settings file"));
        assertArrayEquals(binary, Files.readAllBytes(old()));
        assertFalse(Files.exists(back()));
        assertTrue(tomls().isEmpty());
    }

    @Test
    void aPartiallyFilledOldFileImportsOnlyWhatItHasAndMoves() throws IOException {
        List<String> log = migrateText("# only one\nbuttons.rotate = false\n", true);
        Map<String, String> all = tomls();
        assertEquals("false", all.get("buttons.rotate"));
        assertEquals("true", all.get("buttons.clear"));
        assertEquals("6", all.get("neighbors.max"));
        assertNull(all.get("ui.theme"), "the old file had no client value, so no client toml is written");
        assertEquals(Settings.KEYS.size() - 1, all.size());
        assertTrue(log.contains("Migrated buttons.rotate = false"), log.toString());
        assertFalse(Files.exists(old()));
        assertTrue(Files.exists(back()));
        assertTrue(Files.readString(back()).lines().findFirst().orElse("").contains("to config/barelycrafting-common.toml;"));
    }

    @Test
    void invalidValuesUseDefaultsAndLeaveTheOriginalInPlace() throws IOException {
        String text = String.join("\n", "neighbors.max=abc", "craft.max_per_click=0", "buttons.mirror=maybe", "ui.theme=neon",
                "neighbors.faces=foo", "buttons.balance=FALSE", "mystery.key=1", "");
        List<String> log = migrateText(text, true);
        Map<String, String> all = tomls();
        assertEquals("6", all.get("neighbors.max"));
        assertEquals("1", all.get("craft.max_per_click"));
        assertEquals("true", all.get("buttons.mirror"));
        assertEquals("minecraft", all.get("ui.theme"));
        assertEquals("down,up,north,south,west,east", all.get("neighbors.faces"));
        assertEquals("false", all.get("buttons.balance"));
        String joined = log.toString();
        assertTrue(joined.contains("Invalid value 'abc' for neighbors.max"), joined);
        assertTrue(joined.contains("Clamped craft.max_per_click from 0 to 1"), joined);
        assertTrue(joined.contains("Invalid value 'maybe' for buttons.mirror"), joined);
        assertTrue(joined.contains("Invalid value 'neon' for ui.theme"), joined);
        assertTrue(joined.contains("Ignored unknown key mystery.key"), joined);
        assertTrue(joined.contains("stays where it is"), joined);
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(old()));
        assertFalse(Files.exists(back()));
        assertTrue(ConfigMigration.migrate(dir, true, VERSION).isEmpty(), "the tomls exist now, so nothing runs again");
    }

    @Test
    void clampingAndPartialFacesAreLoggedAndStillMove() throws IOException {
        List<String> log = migrateText("neighbors.max=99\nneighbors.faces=east,foo,East\n", true);
        Map<String, String> all = tomls();
        assertEquals("6", all.get("neighbors.max"));
        assertEquals("east", all.get("neighbors.faces"));
        assertTrue(log.toString().contains("Clamped neighbors.max from 99 to 6"), log.toString());
        assertTrue(log.toString().contains("Dropped unknown side 'foo'"), log.toString());
        assertFalse(Files.exists(old()));
        assertTrue(Files.exists(back()));
    }

    @Test
    void anExistingBackupIsNeverOverwritten() throws IOException {
        Files.writeString(back(), "an earlier backup");
        byte[] original = fixture("v2026.11.3", "edited");
        ConfigMigration.migrate(dir, true, VERSION);
        assertEquals("an earlier backup", Files.readString(back()));
        assertTrue(Files.exists(dir.resolve(ConfigMigration.OLD_FILE + ".back2")));
        assertFalse(Files.exists(old()));
        Files.delete(dir.resolve(ConfigMigration.COMMON_FILE));
        Files.delete(dir.resolve(ConfigMigration.CLIENT_FILE));
        Files.write(old(), original);
        ConfigMigration.migrate(dir, true, VERSION);
        assertTrue(Files.exists(dir.resolve(ConfigMigration.OLD_FILE + ".back3")));
        assertEquals("an earlier backup", Files.readString(back()));
    }

    @Test
    void aSecondLaunchDoesNothing() throws IOException {
        fixture("v2026.11.3", "edited");
        assertFalse(ConfigMigration.migrate(dir, true, VERSION).isEmpty());
        byte[] common = Files.readAllBytes(dir.resolve(ConfigMigration.COMMON_FILE));
        byte[] backup = Files.readAllBytes(back());
        assertTrue(ConfigMigration.migrate(dir, true, VERSION).isEmpty());
        assertArrayEquals(common, Files.readAllBytes(dir.resolve(ConfigMigration.COMMON_FILE)));
        assertArrayEquals(backup, Files.readAllBytes(back()));
        try (var files = Files.list(dir)) {
            assertEquals(3, files.count(), "common toml, client toml and one backup");
        }
    }

    @Test
    void anExistingTomlIsNeverOverwrittenAndTheOldFileStays() throws IOException {
        byte[] original = fixture("v2026.11.3", "edited");
        Files.writeString(dir.resolve(ConfigMigration.COMMON_FILE), "# edited by the player\n");
        Files.writeString(dir.resolve(ConfigMigration.CLIENT_FILE), "# edited by the player\n");
        assertTrue(ConfigMigration.migrate(dir, true, VERSION).isEmpty());
        assertEquals("# edited by the player\n", Files.readString(dir.resolve(ConfigMigration.COMMON_FILE)));
        assertEquals("# edited by the player\n", Files.readString(dir.resolve(ConfigMigration.CLIENT_FILE)));
        assertArrayEquals(original, Files.readAllBytes(old()));
        assertFalse(Files.exists(back()));
    }

    @Test
    void aMissingClientTomlIsImportedEvenWhenTheCommonOneExists() throws IOException {
        fixture("v2026.11.3", "edited");
        Files.writeString(dir.resolve(ConfigMigration.COMMON_FILE), "# edited by the player\n");
        ConfigMigration.migrate(dir, true, VERSION);
        assertEquals("# edited by the player\n", Files.readString(dir.resolve(ConfigMigration.COMMON_FILE)));
        assertEquals("modern", ConfigMigration.readToml(dir.resolve(ConfigMigration.CLIENT_FILE)).get("ui.theme"));
        assertTrue(Files.exists(back()));
    }

    @Test
    void aDedicatedServerNeverWritesTheClientToml() throws IOException {
        fixture("v2026.11.3", "edited");
        ConfigMigration.migrate(dir, false, VERSION);
        assertTrue(Files.exists(dir.resolve(ConfigMigration.COMMON_FILE)));
        assertFalse(Files.exists(dir.resolve(ConfigMigration.CLIENT_FILE)));
        assertTrue(Files.exists(back()));
    }

    @Test
    void migratedValuesReachTheSettings() throws IOException {
        fixture("v2026.11.3", "edited");
        ConfigMigration.migrate(dir, true, VERSION);
        try {
            for (Map.Entry<String, String> e : tomls().entrySet()) {
                Settings.set(e.getKey(), e.getValue());
            }
            assertFalse(Settings.getBool("buttons.rotate"));
            assertEquals(3, Settings.getInt("neighbors.max"));
            assertEquals("east,north", Settings.getString("neighbors.faces"));
        } finally {
            Settings.reset();
        }
        assertNull(Settings.key("nope"));
    }
}
