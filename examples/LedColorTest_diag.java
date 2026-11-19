package org.firstinspires.ftc.teamcode;

import com.eclipse30618.ftc.diaglib.SchemaDiagBus;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "LED Color Test Diag", group = "Student")
public class LedColorTest_diag extends LinearOpMode {
    private static final boolean ENABLE_DIAGNOSTICS = true; // DIAG: set to false in real competitions

    // DIAG: changes this value dynamically, thus, volatile
    private volatile double diag_ledcolor = 0.500; // Green,  0.278 =  Red, 0.611 = Blue, 0 off, 1 = white

    @Override
    public void runOpMode() throws InterruptedException {
        Servo led = hardwareMap.get(Servo.class, "rgb_indicator");

        // DIAG: init diag to null
        final long runId = System.currentTimeMillis() / 1000L;
        SchemaDiagBus diag = null;

        try {
            // DIAG: init diaglib
            if (ENABLE_DIAGNOSTICS) {
                // DIAG: configuration file for dashboard
                diag = new SchemaDiagBus(hardwareMap, "configs/led_color_test_diag.json");
                // DIAG: allow user to dynamically change diag_ledcolor (0 to 1).
                diag.config().registerDouble("diag_ledcolor", () -> diag_ledcolor, v -> diag_ledcolor = v, 0.0, 1.0);
                diag.start(); // DIAG: start service at init stage.
            }

            waitForStart();

            while (opModeIsActive()) {
                // DIAG: store, and use stored value only because diagdashboard can dynamically change value mid loop.
                double ledcolor = diag_ledcolor;

                // change LED color
                led.setPosition(ledcolor);

                // send diagnostics to clients
                if (ENABLE_DIAGNOSTICS) {
                    diag.begin();
                    diag.put("run_id", runId);
                    diag.put("robot_ts_ms", System.currentTimeMillis());
                    diag.put("ledcolor", ledcolor, "%.3f");
                    diag.publish(); // DIAG: send all .put()s data to laptop.
                }
            }
        } finally {
            if (diag != null) diag.close(); // DIAG: stop service at STOP or crash.
        }
    }
}
