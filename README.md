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
[Full app sections from above]

Last updated: March 2026
