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
    implementation "com.eclipse30618.ftc:diaglib:0.2.0"
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

## Grouped diagnostics schema (breaking change)

Only the grouped format is supported. Top-level `fields`, `graphs`, `subscribe`,
and `version` are rejected. Each entry in `groups` contains a `title` and a
`fields` object keyed by telemetry name. A group creates a graph by default;
`"graph": false` keeps its fields in telemetry and CSV without plotting them.
Types default to `double` and units to an empty string. Supported types are
`double`, `long`, and `string`. Use `{"ref": true}` to show a field defined in
another group; references may precede definitions and cannot contain metadata.
Each field must have exactly one definition. All unique fields are subscribed
and logged once. Graph and series order follow group and field order in the file.
The server uses sorted internal indexes and emits values in requested subscription order.

See the bundled `diag_schema.json` for the format. Use the robot's schema as the
source of truth via the dashboard's `--schema` argument. Start a fresh recording
when migrating: CSV column order can change. Existing CSVs are read by column name.
DiagServer 0.2.0 and the grouped-schema dashboard must be deployed together with
the converted robot schema. The old JSON format is not supported.
