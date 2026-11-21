package com.eclipse30618.ftc.diaglib;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

public class SchemaDiagBus implements DiagBus {
    private final SchemaDiagService service;
    private final LiveDiagConfigRegistry configRegistry;
    private boolean enabled = true;
    private boolean errorLogged;

    public SchemaDiagBus(HardwareMap hardwareMap, String schemaPathOrJson) {
        this(hardwareMap, schemaPathOrJson, null);
    }

    public SchemaDiagBus(HardwareMap hardwareMap, String schemaPathOrJson,
            LimelightTunnelManager camera) {
        ConfigRegistry config = new ConfigRegistry();
        this.service = new SchemaDiagService(hardwareMap, schemaPathOrJson, config, camera);
        this.configRegistry = new DiaglibConfigRegistry(config);
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        try {
            service.start();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void begin() {
        if (!enabled) {
            return;
        }
        try {
            service.begin();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, String value) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, double value, String format) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value, format);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void put(String name, long value) {
        if (!enabled) {
            return;
        }
        try {
            service.put(name, value);
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public void publish() {
        if (!enabled) {
            return;
        }
        try {
            service.publish();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    @Override
    public LiveDiagConfigRegistry config() {
        return configRegistry;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void close() {
        if (!enabled) {
            return;
        }
        try {
            service.close();
        } catch (RuntimeException ex) {
            disableWithError(ex);
        }
    }

    private void disableWithError(RuntimeException ex) {
        if (!errorLogged) {
            RobotLog.ee("SchemaDiag", ex, "Diag service error; disabling.");
            errorLogged = true;
        }
        enabled = false;
    }

    private static final class DiaglibConfigRegistry implements LiveDiagConfigRegistry {
        private final ConfigRegistry registry;

        private DiaglibConfigRegistry(ConfigRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void registerDouble(
                String name,
                java.util.function.DoubleSupplier getter,
                java.util.function.DoubleConsumer setter,
                double min,
                double max) {
            registry.registerDouble(name, getter, setter, min, max);
        }
    }
}
