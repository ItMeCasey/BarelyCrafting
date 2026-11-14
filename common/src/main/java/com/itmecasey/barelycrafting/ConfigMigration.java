package com.itmecasey.barelycrafting;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Moves the values of the old hand-written settings file into the loader's {@code .toml} config files, once.
 *
 * <p>Every released version up to 2026.11.3 wrote {@code config/barelycrafting.properties} with the same eleven keys, so that is the
 * only old file there is. The import runs only when a needed toml file does not exist yet and the old file does. In order:
 * <ol>
 * <li>write the toml file(s) (the values the old file had plus the default of every other key; the loader adds the comments);</li>
 * <li>read each toml back and check every migrated value;</li>
 * <li>check that the old file parsed cleanly (no invalid value, nothing unreadable);</li>
 * <li>write {@code barelycrafting.properties.back} (or {@code .back2}, {@code .back3}; an existing backup is never overwritten): one
 * comment line saying where and why it migrated, then the exact original bytes;</li>
 * <li>read the backup back and check it holds the original bytes;</li>
 * <li>only then remove the original.</li>
 * </ol>
 * If any step fails, or the old file is corrupt or has a value that had to fall back to its default, the original stays exactly where
 * it is and the reason is logged. Out-of-range numbers are clamped (logged) and count as readable.
 */
public final class ConfigMigration {
    public static final String OLD_FILE = "barelycrafting.properties";
    public static final String COMMON_FILE = "barelycrafting-common.toml";
    public static final String CLIENT_FILE = "barelycrafting-client.toml";

    private ConfigMigration() {
    }

    /** Whether one value fell back to its default (the old file then stays where it is). */
    private static final class Outcome {
        boolean clean = true;
    }

    /**
     * Imports into the common toml and, when {@code client} is true, the client toml.
     *
     * @param modVersion the mod version, named in the header of the backup
     * @return what was migrated, ignored or fell back, one line each (empty when there was nothing to do)
     */
    public static List<String> migrate(Path configDir, boolean client, String modVersion) {
        List<String> log = new ArrayList<>();
        Path old = configDir.resolve(OLD_FILE);
        Path common = configDir.resolve(COMMON_FILE);
        Path clientToml = configDir.resolve(CLIENT_FILE);
        boolean needCommon = !Files.exists(common);
        boolean needClient = client && !Files.exists(clientToml);
        if ((!needCommon && !needClient) || !Files.isRegularFile(old)) {
            return log;
        }
        byte[] original;
        Properties props = new Properties();
        try {
            original = Files.readAllBytes(old);
            try (Reader reader = new java.io.StringReader(decodeStrict(original))) {
                props.load(reader);
            }
        } catch (IOException | RuntimeException e) {
            log.add("Could not read the old settings file " + old.getFileName() + " (" + e + "); using the defaults, the file stays where it is");
            return log;
        }
        Outcome outcome = new Outcome();
        for (String name : props.stringPropertyNames()) {
            if (Settings.key(name) == null) {
                log.add("Ignored unknown key " + name + " in " + old.getFileName());
            }
        }
        List<Path> written = new ArrayList<>();
        List<Map<String, Object>> expected = new ArrayList<>();
        if (needCommon) {
            if (!write(common, importKeys(props, false, log, outcome), false, log, written, expected)) {
                return log;
            }
        }
        if (needClient) {
            if (!write(clientToml, importKeys(props, true, log, outcome), true, log, written, expected)) {
                return log;
            }
        }
        if (written.isEmpty()) {
            return log;
        }
        for (int i = 0; i < written.size(); i++) {
            if (!verifyToml(written.get(i), expected.get(i), log)) {
                log.add("Kept " + old.getFileName() + " where it is");
                return log;
            }
        }
        if (!outcome.clean) {
            log.add("The old file had a value that could not be read, so " + old.getFileName() + " stays where it is and the defaults are used for those keys");
            return log;
        }
        Path back = freeBackupName(old);
        String header = "# Migrated by Barely Crafting " + modVersion + " on " + LocalDate.now() + " to config/"
                + String.join(" and config/", written.stream().map(p -> p.getFileName().toString()).toList())
                + "; this file is no longer read. Kept as a backup.\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] backup = new byte[headerBytes.length + original.length];
        System.arraycopy(headerBytes, 0, backup, 0, headerBytes.length);
        System.arraycopy(original, 0, backup, headerBytes.length, original.length);
        try {
            Files.write(back, backup, StandardOpenOption.CREATE_NEW);
            byte[] again = Files.readAllBytes(back);
            if (!Arrays.equals(again, backup)) {
                log.add("The backup " + back.getFileName() + " did not match the original; " + old.getFileName() + " stays where it is");
                return log;
            }
            Files.delete(old);
        } catch (IOException e) {
            log.add("Could not finish moving " + old.getFileName() + " to " + back.getFileName() + " (" + e + "); the original stays where it is");
            return log;
        }
        log.add("Migrated config: " + old + " moved to " + back + "; values written to " + String.join(", ", written.stream().map(Path::toString).toList()));
        return log;
    }

    private static Path freeBackupName(Path old) {
        Path back = old.resolveSibling(old.getFileName() + ".back");
        for (int n = 2; Files.exists(back); n++) {
            back = old.resolveSibling(old.getFileName() + ".back" + n);
        }
        return back;
    }

    /** Decodes UTF-8 and refuses malformed bytes, so a binary or damaged file counts as corrupt instead of being half read. */
    private static String decodeStrict(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new IOException("not valid UTF-8 text", e);
        }
    }

    private static Map<String, Object> importKeys(Properties props, boolean clientKeys, List<String> log, Outcome outcome) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Settings.Key key : Settings.KEYS) {
            if (key.client() != clientKeys || !props.containsKey(key.path())) {
                continue;
            }
            String text = props.getProperty(key.path()).trim();
            Object value = convert(key, text, log, outcome);
            out.put(key.path(), value);
            log.add("Migrated " + key.path() + " = " + value);
        }
        return out;
    }

    private static Object convert(Settings.Key key, String text, List<String> log, Outcome outcome) {
        switch (key.kind()) {
            case BOOL -> {
                if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(text);
                }
                return invalid(key, text, "expected true or false", log, outcome);
            }
            case INT -> {
                double number;
                try {
                    number = Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    return invalid(key, text, "expected a number", log, outcome);
                }
                if (Double.isNaN(number) || Double.isInfinite(number)) {
                    return invalid(key, text, "expected a number", log, outcome);
                }
                long rounded = Math.round(number);
                long clamped = Math.max(key.min(), Math.min(key.max(), rounded));
                if (clamped != rounded) {
                    log.add("Clamped " + key.path() + " from " + rounded + " to " + clamped + " (range " + key.min() + " to " + key.max() + ")");
                }
                return (int) clamped;
            }
            case CHOICE -> {
                String lower = text.toLowerCase(Locale.ROOT);
                if (key.choices().contains(lower)) {
                    return lower;
                }
                return invalid(key, text, "allowed: " + String.join(", ", key.choices()), log, outcome);
            }
            default -> {
                List<String> kept = new ArrayList<>();
                boolean dropped = false;
                for (String part : text.split(",")) {
                    String side = part.trim().toLowerCase(Locale.ROOT);
                    if (side.isEmpty()) {
                        continue;
                    }
                    if (Settings.SIDES.contains(side)) {
                        if (!kept.contains(side)) {
                            kept.add(side);
                        }
                    } else {
                        dropped = true;
                        log.add("Dropped unknown side '" + side + "' from " + key.path());
                    }
                }
                if (kept.isEmpty() && dropped) {
                    return invalid(key, text, "no valid side", log, outcome);
                }
                return String.join(",", kept);
            }
        }
    }

    private static Object invalid(Settings.Key key, String text, String expected, List<String> log, Outcome outcome) {
        outcome.clean = false;
        log.add("Invalid value '" + text + "' for " + key.path() + " (" + expected + "); using the default " + key.def());
        return key.def();
    }

    /** Writes the imported values plus the default of every other key of the file, so the loader finds nothing to correct. */
    private static boolean write(Path toml, Map<String, Object> imported, boolean clientKeys, List<String> log, List<Path> written,
            List<Map<String, Object>> expected) {
        if (imported.isEmpty()) {
            return true;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (Settings.Key key : Settings.KEYS) {
            if (key.client() == clientKeys) {
                values.put(key.path(), imported.getOrDefault(key.path(), key.def()));
            }
        }
        try {
            Files.createDirectories(toml.getParent());
            Files.writeString(toml, toToml(values), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.add("Could not write " + toml.getFileName() + " (" + e + "); the defaults are used and the old file stays where it is");
            return false;
        }
        written.add(toml);
        expected.add(values);
        return true;
    }

    /** Reads a written toml back and checks that every value is the one that was meant. */
    private static boolean verifyToml(Path toml, Map<String, Object> expected, List<String> log) {
        try {
            Map<String, String> read = readToml(toml);
            for (Map.Entry<String, Object> e : expected.entrySet()) {
                if (!String.valueOf(e.getValue()).equals(read.get(e.getKey()))) {
                    log.add(toml.getFileName() + " does not hold " + e.getKey() + " = " + e.getValue() + " after writing (found " + read.get(e.getKey()) + ")");
                    return false;
                }
            }
            return true;
        } catch (IOException | RuntimeException e) {
            log.add("Could not read " + toml.getFileName() + " back (" + e + ")");
            return false;
        }
    }

    /** Reads the plain tables and {@code key = value} lines this class writes (quotes removed). */
    static Map<String, String> readToml(Path file) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        String table = "";
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                table = line.substring(1, line.length() - 1);
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                int eq = line.indexOf('=');
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                }
                out.put(table + "." + line.substring(0, eq).trim(), value);
            }
        }
        return out;
    }

    /** The values as toml: one table per key prefix. */
    static String toToml(Map<String, Object> values) {
        Map<String, Map<String, Object>> tables = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            int dot = e.getKey().indexOf('.');
            tables.computeIfAbsent(e.getKey().substring(0, dot), t -> new LinkedHashMap<>()).put(e.getKey().substring(dot + 1), e.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Object>> table : tables.entrySet()) {
            sb.append('[').append(table.getKey()).append("]\n");
            for (Map.Entry<String, Object> e : table.getValue().entrySet()) {
                sb.append(e.getKey()).append(" = ").append(literal(e.getValue())).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String literal(Object value) {
        if (value instanceof String s) {
            return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return String.valueOf(value);
    }
}
