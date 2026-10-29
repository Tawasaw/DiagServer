# DiagServer

DiagServer is the robot-side diagnostics library for FTC robots. The published Android library is `diaglib`.

## Installation

Add Maven Central to the FTC project repositories (It is present by default by FTC SDK):

```gradle
repositories {
    google()
    mavenCentral()
}
```

Add the library dependency in `TeamCode/build.gradle`:

```gradle
dependencies {
    implementation "com.eclipse30618.ftc:diaglib:0.1.3"
}
```

Add a robot-specific schema file at:

```text
TeamCode/src/main/assets/configs/diag_schema.json
```

Copyable schema examples are in `examples/`:

```text
examples/diag_schema_example.json      Minimal schema for the Java examples
examples/diag_schema_drivetrain.json   Drivetrain-focused schema
```

Use the diagnostics bus from an OpMode:

```java
import com.eclipse30618.ftc.diaglib.DiagBus;
import com.eclipse30618.ftc.diaglib.SchemaDiagBus;

DiagBus diagBus = new SchemaDiagBus(hardwareMap, "configs/diag_schema.json");
diagBus.start();

diagBus.begin();
diagBus.put("robot_ts_ms", System.currentTimeMillis());
diagBus.put("front_left_power", frontLeft.getPower(), "%.3f");
diagBus.publish();
```

Live config setters are applied from the diagnostics socket thread. If the robot
loop reads the same values, store them in `volatile` fields, synchronized accessors,
or atomic holders.

## Project Layout

```text
diaglib/   Android library published as com.eclipse30618.ftc:diaglib
examples/  Copyable FTC OpMode examples
```

## Limelight recording tunnel

`LimelightTunnelManager.createDefault()` forwards Control Hub ports 5800
(annotated preview), 5801 (GUI), **5802 (raw MJPEG)**, 5805 (websocket),
and 5807 (HTTP API) to the USB camera at `172.29.0.1`. Raw-video forwarding
is available starting with **diaglib 0.1.3**. Start and close the manager
with the OpMode lifecycle as shown in `examples/`.

DiagDashboard's `--record-limelight` option records port 5802 on the laptop,
while its existing preview continues using 5800. Tunnels relay bytes on
background threads; FFmpeg and recording storage run on the laptop. Two
active streams consume additional USB/Wi-Fi bandwidth and Control Hub
resources. Measure robot loop timing and result age on the physical robot.
Starting the manager alone does not request camera frames: clients must connect.
