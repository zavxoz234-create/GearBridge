package com.gearbridge.app

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class DeviceRow(val device: BluetoothDevice, val rssi: Int, val transport: String)

class MainActivity : ComponentActivity() {
    private val found = mutableStateMapOf<String, DeviceRow>()
    private var scanning by mutableStateOf(false)
    private var message by mutableStateOf("Ready to scan")
    private var gattText by mutableStateOf("No GATT inspection yet")
    private var gatt: BluetoothGatt? = null
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it }) startScan() else message = "Nearby devices permission denied"
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                @Suppress("DEPRECATION") val d = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                @Suppress("DEPRECATION") val r = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                add(d, r, "Classic")
            }
        }
    }
    private val bleCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) = add(result.device, result.rssi, "BLE")
        override fun onScanFailed(code: Int) { message = "BLE scan failed ($code)" }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { gattText = "GATT connected; reading services…" }; g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) runOnUiThread { gattText = "Disconnected (status $status)" }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val result = if (status == BluetoothGatt.GATT_SUCCESS) g.services.joinToString("\n") { s ->
                s.uuid.toString() + s.characteristics.joinToString("", prefix = "\n  ") { c -> "${c.uuid} properties=${c.properties}" }
            } else "Service discovery failed ($status)"
            runOnUiThread { gattText = result }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF70E0C0), background = Color(0xFF101513), surface = Color(0xFF19211E))) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("GEARBRIDGE", color = Color(0xFF70E0C0)); Text("Device discovery", style = MaterialTheme.typography.headlineMedium)
                Text("Scan nearby Bluetooth devices and inspect read-only GATT metadata.")
                Spacer(Modifier.height(12.dp)); Button(onClick = { if (scanning) stopScan() else requestScan() }) { Text(if (scanning) "Stop scan" else "Scan nearby devices") }
                Text(message); Text("DEVICES · ${found.size}")
                LazyColumn(Modifier.weight(1f)) { items(found.values.toList()) { row ->
                    val name = try { row.device.name ?: "Unknown device" } catch (_: SecurityException) { "Nearby device" }
                    val address = try { row.device.address } catch (_: SecurityException) { "Address unavailable" }
                    Column(Modifier.fillMaxWidth().clickable { inspect(row.device) }.padding(12.dp)) {
                        Text(name); Text("$address · ${row.rssi} dBm · ${row.transport}")
                    }
                } }
                Text(gattText, style = MaterialTheme.typography.bodySmall)
                Text("Discovery or GATT does not mean Samsung pairing or authentication.", style = MaterialTheme.typography.labelSmall)
            }
        } }
    }
    private fun requestScan() {
        val p = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = p.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startScan() else permissionRequest.launch(missing.toTypedArray())
    }
    private fun startScan() {
        val a = adapter ?: run { message = "Bluetooth unavailable"; return }
        if (!a.isEnabled) { message = "Turn on Bluetooth and scan again"; return }
        found.clear(); scanning = true; message = "Scanning BLE and Bluetooth Classic for 12 seconds…"
        try {
            ContextCompat.registerReceiver(this, receiver, IntentFilter(BluetoothDevice.ACTION_FOUND), ContextCompat.RECEIVER_EXPORTED)
            a.startDiscovery(); a.bluetoothLeScanner?.startScan(bleCallback)
            window.decorView.postDelayed({ if (scanning) stopScan() }, 12_000)
        } catch (e: Exception) { scanning = false; message = "Scan unavailable: ${e.message}" }
    }
    private fun stopScan() { try { adapter?.cancelDiscovery(); adapter?.bluetoothLeScanner?.stopScan(bleCallback); unregisterReceiver(receiver) } catch (_: Exception) {}; scanning = false; message = "Scan complete · ${found.size} devices" }
    private fun add(d: BluetoothDevice, rssi: Int, via: String) {
        val id = try { d.address } catch (_: Exception) { d.hashCode().toString() }
        val old = found[id]; found[id] = DeviceRow(d, rssi, if (old == null || old.transport == via) via else "BLE + Classic")
    }
    private fun inspect(d: BluetoothDevice) { try { gatt?.close(); gattText = "Connecting for GATT inspection…"; gatt = d.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE) } catch (e: Exception) { gattText = "GATT unavailable: ${e.message}" } }
    override fun onDestroy() { try { adapter?.bluetoothLeScanner?.stopScan(bleCallback); adapter?.cancelDiscovery(); gatt?.close() } catch (_: Exception) {}; super.onDestroy() }
}
