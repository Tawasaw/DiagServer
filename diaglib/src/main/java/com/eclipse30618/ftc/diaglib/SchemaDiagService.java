package com.eclipse30618.ftc.diaglib;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

/**
 * Schema-driven diag publisher with name/value puts.
 *
 * This is the "robot server" side. It accepts field names and values,
 * checks them against the JSON schema, and sends the data to the laptop.
 */
public class SchemaDiagService implements AutoCloseable {
    private final DiagSchema schema;
    private final FieldCatalog catalog;
    private final DiagServer server;
    private final ConfigRegistry configRegistry;
    private DiagSnapshotBuilder builder;
    private boolean started;

    /**
     * Create a diag service backed by a schema file or raw JSON string.
     */
    public SchemaDiagService(HardwareMap hardwareMap, String schemaPathOrJson) {
        this(hardwareMap, schemaPathOrJson, null);
    }

    /**
     * Create a diag service with optional live-config registry.
     */
    public SchemaDiagService(
            HardwareMap hardwareMap, String schemaPathOrJson, ConfigRegistry configRegistry) {
        this(hardwareMap, schemaPathOrJson, configRegistry, null);
    }

    SchemaDiagService(HardwareMap hardwareMap, String schemaPathOrJson,
            ConfigRegistry configRegistry, LimelightTunnelManager camera) {
        // Load the schema (allowed fields + port) from JSON.
        this.schema = DiagSchema.fromConfig(hardwareMap, schemaPathOrJson);
        // Build a fixed list of fields so every data line matches the same order.
        this.catalog = new FieldCatalog();
        for (SchemaField field : schema.getFields()) {
            catalog.add(field.name, field.type, field.unit);
        }
        this.configRegistry = configRegistry;
        // Start the TCP server that the laptop will connect to.
        this.server =
                new DiagServer(
                        schema.getPort(), catalog, configRegistry, schema.getMaxRateHz(), schema, camera);
    }

    /**
     * Start the TCP server if it is not already running.
     */
    public void start() {
        if (started) {
            return;
        }
        // Begin listening for laptop connections.
        server.start();
        started = true;
    }

    /**
     * Begin a new loop snapshot; call before put(...).
     */
    public void begin() {
        // Start a new "snapshot" for this loop.
        builder = new DiagSnapshotBuilder(catalog.size());
    }

    /**
     * Put a string value (for text/status fields).
     */
    public void put(String name, String value) {
        // This overload is for text values (status, names, notes).
        requireBuilder();
        Integer idx = schema.indexOf(name);
        if (idx == null) {
            handleUnknown(name);
            return;
        }
        // Save the value in the correct position for this field.
        builder.set(idx, value);
    }

    /**
     * Put a numeric value with a specific format (ex: "%.2f").
     */
    public void put(String name, double value, String format) {
        // This overload is for numbers where you want a format like "%.2f".
        requireBuilder();
        Integer idx = schema.indexOf(name);
        if (idx == null) {
            handleUnknown(name);
            return;
        }
        // Format the number and save it into the snapshot.
        builder.set(idx, value, format);
    }

    /**
     * Put a long value (timestamps, counters).
     */
    public void put(String name, long value) {
        // This overload is for timestamps or counters (no decimals).
        requireBuilder();
        put(name, String.valueOf(value));
    }

    /**
     * Publish the current snapshot to connected dashboard clients.
     */
    public void publish() {
        if (builder == null) {
            return;
        }
        // Send the latest snapshot to all connected clients.
        server.setSnapshot(builder.build());
    }

    /**
     * Stop the server and close all client sessions.
     */
    @Override
    public void close() {
        // Stop the server cleanly.
        server.close();
    }

    private void handleUnknown(String name) {
        if (schema.isStrict()) {
            throw new IllegalArgumentException("Diag field not in schema: " + name);
        }
        // Non-strict mode: warn and ignore the field.
        RobotLog.ww("SchemaDiag", "Unknown field (ignored): %s", name);
    }

    private void requireBuilder() {
        if (builder == null) {
            // This catches mistakes like put(...) before begin().
            throw new IllegalStateException("Call begin() before putting diag values.");
        }
    }
}
