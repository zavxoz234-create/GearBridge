# GearBridge

An experimental Android companion project investigating Samsung Gear Sport support. The first milestone provides Bluetooth discovery and read-only BLE GATT diagnostics. Discovery, bonding, GATT transport, Samsung Accessory Protocol (SAP), and application pairing are separate states. The app never reports a verified watch session based on discovery or GATT alone.

## Current milestone

- BLE scan and Bluetooth Classic discovery for 12 seconds, with cancellation and de-duplication.
- Nearby Devices permission on Android 12+; fine location for Android 11 and earlier.
- Device name, address, RSSI, and discovery transport where Android exposes them.
- User-initiated GATT connection and service/characteristic metadata inspection. No writes or custom protocol commands.

## Build

Open in Android Studio with JDK 17 and Android SDK Platform 35. Let Gradle sync, then run the `app` configuration on a physical phone. Bluetooth testing requires a physical device. Build a debug APK using Android Studio's Build APK action or `gradle assembleDebug` with Gradle installed.

## Diagnostics

1. Grant Nearby Devices permission.
2. Enable Bluetooth and put the Gear Sport in its normal discoverable/pairing state.
3. Scan and note identity, RSSI, and discovery method.
4. Tap a device to inspect discovered GATT UUIDs and properties.
5. Discovery or a GATT link does not establish Samsung authentication, notification relay, battery access, or SAP support.

## Research and limitations

Samsung's Accessory SDK lists Gear Sport as supported, but requires Samsung Accessory Service Framework on the phone. Samsung describes a framework spanning Bluetooth Classic, BLE, and Wi-Fi; its public documentation does not provide a generic Gear Sport pairing handshake or universal GATT UUIDs. This project does not guess packet formats or transmit commands.

Samsung's compatibility table lists Gear Sport support through Android 13. Pixel 10 / Android 16 compatibility is not established. SAP, authentication, notifications, calls, battery/time sync, reconnection, health sync, and verified pairing are not implemented. No watch was available for hardware verification.

## Sources

- [Samsung Accessory SDK overview](https://developer.samsung.com/galaxy-accessory/overview.html)
- [Samsung Accessory SDK Android guide](https://developer.samsung.com/galaxy-watch-tizen/creating-your-first-app/net-companion/use-accessory.html)
- [Samsung phone compatibility](https://www.samsung.com/us/support/answer/ANS10004668/)
- [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Android BLE overview](https://source.android.com/docs/core/connectivity/bluetooth/ble)
