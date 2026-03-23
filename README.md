# Sabah Old Boundary Stakeout App + DIY RTK System

**Full living specification** – updated March 2026 with exact hardware build.

## Hardware (Exact Current Build)
Modules
Two identical Unicore UM980 (RTK UH98 v1.0.1 breakout boards with SMA/TNC antenna connectors).
Antennas
Base: High-gain white dome antenna (TNC-K, ≥5.5 dBi, 40 dB LNA, IP67, full L1/L2/L5 + BDS B1/B2/B3 + GLONASS + Galileo + L-band, RHCP).
Rover: Quadrifilar helical antenna (HX-901 style, SMA-J, 35±3 dB LNA, RHCP, axial ratio <3 dB, 43.8 × 42.2 mm) — optimised for multipath rejection in thick jungle foliage.
Data Link
Two DX-LR02 (ASR6601) 433 MHz LoRa modules (22 dBm, UART transparent mode, SMA antenna jack, M0 & M1 tied to GND).
Microcontrollers
Two ESP32-S3 boards (one base, one rover).
Power
One 18650 dual-battery shield (5 V/3 A rail powers UM980 + ESP32 VIN; 3.3 V/1 A rail powers LoRa VCC).
Exact Wiring (Rover Side – Mirror on Base)
Connection
From
To
Notes
LoRa TXD
LoRa module
ESP32 GPIO16 (RX)
UART passthrough
LoRa RXD
LoRa module
ESP32 GPIO17 (TX)
UART passthrough
UM980 corrections input
ESP32 GPIO18 (TX)
UM980 TTL_RXD2 (COM2 input)
RTCM forwarding
UM980 NMEA output
UM980 TTL_TXD1 (COM1)
ESP32 GPIO8 (RX)
GGA/RMC at 1 Hz
All GNDs
Common
—
—
Power
5 V rail
UM980 + ESP32 VIN
—
LoRa power
3.3 V rail
LoRa VCC
—
Rover Extra Output
ESP32 also outputs NMEA via Bluetooth Classic SPP (device name “RoverRTK”) to Samsung A13.
Data Flow
Base UM980 (fixed/survey-in mode) → RTCM 3.x (1006/1033/1074/1084/1094/1124 @ 1 Hz) on COM2 → ESP32 → LoRa (transparent).
Rover LoRa receives RTCM → ESP32 forwards directly to UM980 TTL_RXD2.
Rover UM980 (rover mode) computes double-difference RTK → GGA/RMC NMEA @ 1 Hz on COM1 → ESP32 → Bluetooth SPP → Phone (or SW Maps / GNSS Status).
Configuration (Already Applied & Verified)
Base: MODE BASE (survey-in or fixed) + RTCM output on COM2.
Rover: MODE ROVER + GPGGA 1 + GPRMC 1.
Both LoRa: 115200 baud, same channel, 2.4 kbps air rate.
NMEA clean: Only GGA + RMC + GSA at 1 Hz (no GSV).
This setup delivers 1–2 cm accuracy under canopy (multi-frequency + helical antenna + LoRa link <5–10 km line-of-sight).]

## App Requirements
Core Workflow (Your Original Request)
Enter points (name + optional initial coordinates).
Enter connections: bearing dd.mmss + distance (m).
Each connection has isSkewBearing flag (Sabah RSO grid vs true bearing).
Fix minimum 2 physical marks with live GNSS (RTK only).
On every fix: entire old plan automatically scales, rotates, and translates (4-parameter similarity transformation) so all points “follow” the fixed marks. Fixed points stay locked.
Data Model (Room – Final Version)
@Entity
data class Point(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    var planEast: Double = 0.0,      // metres after Bowditch + transformation
    var planNorth: Double = 0.0,
    var fixedLat: Double? = null,    // WGS84 from RTK
    var fixedLon: Double? = null,
    var isFixed: Boolean = false
)

@Entity
data class Connection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPointId: Long,
    val toPointId: Long,
    val bearingDMS: String,          // "123.4555"
    val distanceM: Double,
    val isSkewBearing: Boolean = false   // Sabah-specific
)
Bearing Parser
(Handles dd.mmss + skew flag – code as before.)
Plan Building + Misclosure Handling (1800s Plans)
Graph traversal → raw relative East/North.
Automatic Bowditch adjustment on all loops (proportional to leg length).
Optional full least-squares.
Report: misclosure ratio (e.g. 1:850), post-adjustment RMS, warning if >1:1000.
Handles huge 1800s errors perfectly.
Core Transformation (Scale & Follow)
Runs every time ≥2 points fixed.
Least-squares similarity (scale + rotation + translation) using centroids.
Computes residuals & overall RMS for all fixed points.
Colour-coded: Green <15 cm, Yellow 15–40 cm, Red >40 cm.
Fixed points locked; others update instantly.
Respects skew-bearing flag for convergence if RSO coords imported.
GNSS / BLE / RTK Integration
Connect to “RoverRTK” Bluetooth SPP → parse clean 1 Hz NMEA.
RTK quality gate: “Fix Point” button disabled unless GGA fix type = 4/5 (RTK Fixed) and accuracy < 3 cm.
Auto-average 30–60 seconds on fix.
Big banner: “RTK Fixed 1–2 cm” (or warning if autonomous).
Live stakeout: distance + bearing (Vincenty) + arrow + voice guidance.
Import / Export
KML/KMZ full support: Points, LineStrings, closed Polygons (auto-creates points + connections + loop).
Detects WGS84 vs RSO → datum handling prompt.
CSV import/export.
KML export for surveyor handoff.
UI (Jetpack Compose + OSMdroid)
Points list with live distance/bearing/residuals.
Add connection screen with skew checkbox.
KML polygon import screen.
Live stakeout view (big arrow + voice).
Transformation report screen (RMS map overlay).
Tech Stack
Kotlin + Compose + Room + NMEA parser + OSMdroid + Vincenty geodesy + Google KML parser.

Last updated: March 2026
