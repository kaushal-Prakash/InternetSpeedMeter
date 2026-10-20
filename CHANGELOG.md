# Changelog

All notable changes to the Internet Speed Meter project will be documented in this file.

## [1.0.0] - Initial Release

### Added
- Implemented persistent foreground service (`SpeedMeterService`) for continuous internet speed monitoring.
- Integrated `TrafficStats` API for high-precision, low-overhead download and upload rate calculations.
- Added real-time notification drawer speed display with daily Wi-Fi and Mobile data totals.
- Added dynamic status bar icon bitmap generator displaying numeric speed directly in status bar.
- Developed custom `SpeedGraphView` using native Android Canvas for live speed graph rendering.
- Integrated `AppPreferences` storage for managing daily/monthly data statistics and app preferences.
- Added user-configurable speed units: Bytes per second (KB/s, MB/s) and Bits per second (Kbps, Mbps).
- Added flexible screen-off behaviors (Keep Active, Slow Down to 10s, Pause Sampling) to minimize battery impact.
- Added `BootReceiver` for auto-starting the foreground service upon system boot and package update.
- Implemented Quick Settings Tile (`SpeedTileService`) for instant service toggling from the quick settings shade.
- Added OEM background permission setup dialog targeting Xiaomi, Samsung, Huawei, Oppo, Vivo, and OnePlus devices.
- Implemented Battery Optimization exemption prompt via `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Added Material 3 dashboard layout with live connection state indicator and data usage summary cards.
