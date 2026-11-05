package com.eclipse30618.ftc.diaglib;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Loads a diag field schema from JSON.
 *
 * The schema defines which fields are allowed and their order.
 * Think of it like a class roster: only names in the roster are allowed.
 */
public class DiagSchema {
    private final int port;
    private final boolean strict;
    private final int maxRateHz;
    private final List<SchemaField> fields;
    private final Map<String, Integer> indexByName;

    private DiagSchema(int port, boolean strict, int maxRateHz, List<SchemaField> fields) {
        this.port = port;
        this.strict = strict;
        this.maxRateHz = maxRateHz;
        this.fields = fields;
        this.indexByName = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            indexByName.put(fields.get(i).name, i);
        }
    }

    /**
     * Load a schema from assets, a local file, or raw JSON text.
     */
    public static DiagSchema fromConfig(HardwareMap hardwareMap, String pathOrJson) {
        // Load the JSON text, then parse it into fields we can use in code.
        JSONObject json = loadConfig(hardwareMap, pathOrJson);
        int port = json.optInt("port", 5599);
        boolean strict = json.optBoolean("strict", false);
        int maxRateHz = json.optInt("max_rate_hz", 100);
        if (maxRateHz <= 0) {
            maxRateHz = 100;
        }
        for (String old : new String[] {"fields", "graphs", "subscribe", "version"}) {
            if (json.has(old)) throw new IllegalArgumentException("Unsupported schema key: " + old + "; use groups");
        }
        JSONObject groups = json.optJSONObject("groups");
        if (groups == null || groups.length() == 0) throw new IllegalArgumentException("Schema requires nonempty groups");
        java.util.Map<String, SchemaField> definitions = new java.util.TreeMap<>();
        java.util.Set<String> references = new java.util.TreeSet<>();
        java.util.Iterator<String> groupKeys = groups.keys();
        while (groupKeys.hasNext()) {
            String id = groupKeys.next();
            JSONObject group = groups.optJSONObject(id);
            if (id.isEmpty() || group == null) throw new IllegalArgumentException("Invalid group: " + id);
            checkKeys(group, "title", "graph", "fields");
            if (group.has("graph") && !(group.opt("graph") instanceof Boolean)) throw new IllegalArgumentException("graph must be boolean: " + id);
            if (group.has("title") && !(group.opt("title") instanceof String)) throw new IllegalArgumentException("title must be text: " + id);
            JSONObject entries = group.optJSONObject("fields");
            if (entries == null || entries.length() == 0) throw new IllegalArgumentException("Group requires fields: " + id);
            java.util.Iterator<String> names = entries.keys();
            while (names.hasNext()) {
                String name = names.next();
                JSONObject field = entries.optJSONObject(name);
                if (name.isEmpty() || name.matches("(?s).*[;,\\r\\n].*") || field == null) throw new IllegalArgumentException("Invalid field: " + name);
                if (field.has("ref")) {
                    if (field.length() != 1 || !Boolean.TRUE.equals(field.opt("ref"))) throw new IllegalArgumentException("Reference must contain only ref: true: " + name);
                    references.add(name);
                    continue;
                }
                checkKeys(field, "type", "unit");
                Object type = field.has("type") ? field.opt("type") : "double";
                Object unit = field.has("unit") ? field.opt("unit") : "";
                if (!("double".equals(type) || "long".equals(type) || "string".equals(type)) || !(unit instanceof String)) throw new IllegalArgumentException("Invalid type or unit: " + name);
                if (definitions.containsKey(name)) throw new IllegalArgumentException("Duplicate field definition: " + name);
                definitions.put(name, new SchemaField(name, (String) type, (String) unit));
            }
        }
        references.removeAll(definitions.keySet());
        if (!references.isEmpty()) throw new IllegalArgumentException("Undefined field references: " + references);
        // Sorted internal indexes are independent of client-requested CSV order.
        List<SchemaField> fields = new ArrayList<>(definitions.values());
        return new DiagSchema(port, strict, maxRateHz, fields);
    }

    private static void checkKeys(JSONObject object, String... allowed) {
        java.util.Set<String> keys = new java.util.HashSet<>(java.util.Arrays.asList(allowed));
        java.util.Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!keys.contains(key)) throw new IllegalArgumentException("Unknown schema setting: " + key);
        }
    }

    /**
     * TCP port the diag server should listen on.
     */
    public int getPort() {
        // TCP port where the robot will listen for the laptop connection.
        return port;
    }

    /**
     * Whether to throw on unknown fields (strict mode).
     */
    public boolean isStrict() {
        // Strict mode means: crash if you try to publish a field not in the schema.
        return strict;
    }

    /**
     * Maximum diag send rate per client (Hz).
     */
    public int getMaxRateHz() {
        return maxRateHz;
    }

    /**
     * Ordered list of schema fields defined in the JSON file.
     */
    public List<SchemaField> getFields() {
        // Full list of fields in deterministic name order.
        return fields;
    }

    /**
     * Resolve a field name to its index in the schema, or null if missing.
     */
    public Integer indexOf(String name) {
        // Fast lookup so we can fill values in a fixed array.
        return indexByName.get(name);
    }

    /**
     * Clean a string so it is safe to put in one CSV column.
     */
    public static String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", ";").replace("\n", " ").replace("\r", " ");
    }

    private static JSONObject loadConfig(HardwareMap hardwareMap, String pathOrJson) {
        // If the string starts with "{", treat it as raw JSON.
        String trimmed = pathOrJson == null ? "" : pathOrJson.trim();
        if (trimmed.startsWith("{")) {
            try {
                return new JSONObject(trimmed);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid schema JSON string.", e);
            }
        }
        // Otherwise, treat it like a file path (assets or disk).
        String jsonText = readConfigText(hardwareMap, trimmed);
        try {
            return new JSONObject(jsonText);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema JSON file.", e);
        }
    }

    private static String readConfigText(HardwareMap hardwareMap, String path) {
        /*
         * Load order:
         * 1) Android assets (TeamCode/src/main/assets).
         * 2) App files dir (/data/user/0/.../files) when a file is pushed manually.
         */
        IOException lastException = null;
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Config path is empty.");
        }
        try (InputStream in = hardwareMap.appContext.getAssets().open(path)) {
            // First try assets (good for on-bot files).
            return readAll(in);
        } catch (IOException e) {
            lastException = e;
        }

        File file = new File(path);
        if (!file.isAbsolute()) {
            // If it's a relative path, search inside the app's files folder.
            file = new File(hardwareMap.appContext.getFilesDir(), path);
        }
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load diag schema: " + path, lastException);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        // Read all text from the input stream into one big String.
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}
