package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import com.eclipse30618.ftc.diaglib.SchemaDiagBus;
import com.eclipse30618.ftc.diaglib.DiagTelemetry;
import com.eclipse30618.ftc.diaglib.DiagBus;

/**
 * Example OpMode using the DiagTelemetry wrapper + SchemaDiagBus.
 *
 * Key idea: keep using the normal SDK telemetry API (addData/update),
 * while the wrapper mirrors those fields into the schema-driven diagnostics server.
 *
 * Rename this file if needed so the public class name matches the file name.
 */
@TeleOp(name = "ExampleDiagTelemetryWrapper", group = "Examples")
public class ExampleDiagTelemetryWrapper extends LinearOpMode {
    private static final String DIAG_SCHEMA_PATH = "configs/diag_schema.json";
    // Live-tunable values exposed to the dashboard.
    private volatile double shooterKp = 0.015;
    private volatile double turretLockGain = 3.0;
    private DiagTelemetry diagTelemetry;

    @Override
    public void runOpMode() {
        DcMotorEx frontLeft = hardwareMap.get(DcMotorEx.class, "FrontLeft");
        IMU imu = hardwareMap.get(IMU.class, "imu");

        // 1) Build the schema-driven diagnostics bus.
        DiagBus diagBus = new SchemaDiagBus(hardwareMap, DIAG_SCHEMA_PATH);
        // 2) Register tunables that the dashboard can update live via SET.
        diagBus
                .config()
                .registerDouble("shooter_kp", () -> shooterKp, v -> shooterKp = v, 0.0, 1.0);
        diagBus
                .config()
                .registerDouble("turret_lock_gain", () -> turretLockGain, v -> turretLockGain = v, 0.0, 10.0);
        // 3) Wrap the SDK telemetry so addData/update also publishes to the bus.
        diagTelemetry = new DiagTelemetry(telemetry, diagBus);
        telemetry = diagTelemetry;

        final long runId = System.currentTimeMillis() / 1000L;
        telemetry.addData("run_id", "%.0f", (double) runId);
        telemetry.addData("robot_ts_ms", "%.0f", (double) System.currentTimeMillis());
        telemetry.addLine("Ready for start");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // Standard SDK telemetry calls (also mirrored to the schema bus).
            telemetry.addData("run_id", "%.0f", (double) runId);
            telemetry.addData("robot_ts_ms", "%.0f", (double) System.currentTimeMillis());
            telemetry.addData("front_left_power", "%.3f", frontLeft.getPower());
            telemetry.addData("imu_yaw_deg", "%.2f",
                    imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
            // Live-config values still show up as normal fields.
            telemetry.addData("shooter_kp", "%.4f", shooterKp);
            telemetry.addData("turret_lock_gain", "%.2f", turretLockGain);
            telemetry.update();
        }

        diagTelemetry.close();
    }
}
