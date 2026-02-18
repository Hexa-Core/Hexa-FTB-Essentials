package dev.ftb.mods.ftbessentials.kitclaim;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KitClaimConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "hexacore-kitclaim.toml";

    private static final String DEFAULT_TOML = """
publicKits = [\"start\", \"food\"]

[kitPermissions]
premium = \"hexacore.kit.claim.premium\"
deluxem = \"hexacore.kit.claim.deluxem\"
ultra = \"hexacore.kit.claim.ultra\"
legend = \"hexacore.kit.claim.legend\"
""";

    private static volatile ConfigData DATA = ConfigData.defaults();
    private static volatile boolean loaded = false;

    private KitClaimConfig() {
    }

    public static void load(Path configDir) {
        if (configDir == null) {
            return;
        }
        Path path = configDir.resolve(FILE_NAME);
        ensureExists(path);
        DATA = parse(path);
        loaded = true;
    }

    public static ConfigData get() {
        return DATA;
    }

    private static void ensureExists(Path path) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, DEFAULT_TOML, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to create default config at {}", path, e);
        }
    }

    private static ConfigData parse(Path path) {
        List<String> publicKits = new ArrayList<>();
        Map<String, String> kitPermissions = new HashMap<>();

        boolean inPerms = false;

        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    inPerms = line.equalsIgnoreCase("[kitPermissions]");
                    continue;
                }

                if (line.startsWith("publicKits")) {
                    List<String> parsed = parseList(line);
                    if (parsed != null) {
                        publicKits = parsed;
                    }
                    continue;
                }

                if (inPerms) {
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        String kit = normalize(key);
                        String perm = stripQuotes(value);
                        if (!kit.isEmpty() && !perm.isEmpty()) {
                            kitPermissions.put(kit, perm);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read config at {}", path, e);
            return ConfigData.defaults();
        }

        if (publicKits.isEmpty()) {
            publicKits = ConfigData.defaults().publicKits();
        }

        if (kitPermissions.isEmpty()) {
            kitPermissions = ConfigData.defaults().kitPermissions();
        }

        return new ConfigData(publicKits, kitPermissions);
    }

    private static List<String> parseList(String line) {
        int start = line.indexOf('[');
        int end = line.indexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        String inner = line.substring(start + 1, end).trim();
        if (inner.isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = inner.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String item = stripQuotes(part.trim());
            if (!item.isEmpty()) {
                result.add(normalize(item));
            }
        }
        return result;
    }

    private static String stripQuotes(String value) {
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ConfigData(List<String> publicKits, Map<String, String> kitPermissions) {
        public ConfigData {
            publicKits = List.copyOf(publicKits);
            kitPermissions = Map.copyOf(kitPermissions);
        }

        public static ConfigData defaults() {
            List<String> publicKits = List.of("start", "food");
            Map<String, String> kitPermissions = new HashMap<>();
            kitPermissions.put("premium", "hexacore.kit.claim.premium");
            kitPermissions.put("deluxem", "hexacore.kit.claim.deluxem");
            kitPermissions.put("ultra", "hexacore.kit.claim.ultra");
            kitPermissions.put("legend", "hexacore.kit.claim.legend");
            return new ConfigData(publicKits, kitPermissions);
        }
    }
}
