package com.idnp2024a.beaconscanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.location.LocationManagerCompat
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.Beacon
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.BeaconParser
import com.idnp2024a.beaconscanner.BeaconScanerLibrary.BleScanCallback
import com.idnp2024a.beaconscanner.TrilateracionLibrary.LinearLeastSquaresSolver
import com.idnp2024a.beaconscanner.TrilateracionLibrary.NonLinearLeastSquaresSolver
import com.idnp2024a.beaconscanner.TrilateracionLibrary.TrilaterationFunction
import com.idnp2024a.beaconscanner.permissions.BTPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer


class MainActivityBLE : AppCompatActivity() {

    private val TAG: String = "MainActivityBLE"
    private var alertDialog: AlertDialog? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var btScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var txtMessage: TextView
    private lateinit var txtMessage2: TextView
    private lateinit var txtMessage3: TextView

    private val permissionManager = PermissionManager.from(this)
    private val beacons = HashMap<String, Beacon>();
    private val _resultBeacons = MutableStateFlow("No beacons Detected")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ble)
        initUI()
        BTPermissions(this).check()
        initBluetooth()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initUI() {
        val btnAdvertising = findViewById<Button>(R.id.btnAdversting)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        txtMessage = findViewById(R.id.txtMessage)
        txtMessage2 = findViewById(R.id.txtMessage2)
        txtMessage3 = findViewById(R.id.txtMessage3)

        val scanCallBack  = createBleScanCallback();
        btnAdvertising.setOnClickListener { handleAdvertisingClick() }
        btnStart.setOnClickListener { handleStartClick(scanCallBack) }
        btnStop.setOnClickListener { handleStopClick(scanCallBack) }
    }

    private fun handleAdvertisingClick() {
        Log.i(TAG, "Press start advertising button");

    }

    private fun handleStartClick(bleScanCallback: BleScanCallback) {
        Log.i(TAG, "Press start scan button");
        if (!isLocationEnabled() || !isBluetoothEnabled()) {
            showPermissionDialog("Servicios no activados", "La localizacion y el Bluetooth tienen que estar activos");
            return
        }

        bluetoothScanStart(bleScanCallback)
    }

    private fun isLocationEnabled(): Boolean {
        Log.i(TAG, "Verificando localizacion activo");
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager;
        Log.d(TAG, "Localizacion activado: "+ LocationManagerCompat.isLocationEnabled(locationManager));
        return LocationManagerCompat.isLocationEnabled(locationManager);
    }

    private fun isBluetoothEnabled(): Boolean {
        Log.i(TAG, "Verificando Bluetooth activo");
        Log.d(TAG, "Bluetooth activado: "+ bluetoothAdapter.isEnabled);
        return bluetoothAdapter.isEnabled;
    }

    private fun handleStopClick(bleScanCallback: BleScanCallback) {
        Log.i(TAG, "Press stop scan button");
        bluetoothScanStop(bleScanCallback)
    }

    private fun initBluetooth() {
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter != null) {
            btScanner = bluetoothAdapter.bluetoothLeScanner
        } else {
            Log.d(TAG, "BluetoothAdapter is null")
        }
    }

    private fun bluetoothScanStart(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "Starting Bluetooth scan...")
        if (btScanner != null) {
            permissionManager
                .request(Permission.Bluetooth)
                .rationale("Bluetooth permission is needed")
                .checkPermission { isGranted ->
                    if (isGranted) {
                        Log.d(TAG, "Permissions granted, starting scan.")
//                        val manufacturerId = 0x0118
                        val manufacturerId = 0x004C
                        val macAddress = "57:7C:45:AC:ED:FE"
                        val scanFilter = ScanFilter.Builder()
                            .setManufacturerData(manufacturerId, null) // Ejemplo para iBeacon
//                            .setDeviceAddress(macAddress)
                            .build()
                        val scanSettings = ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build()
                        btScanner.startScan(listOf(scanFilter), scanSettings,bleScanCallback)
                    } else {
                        Log.d(TAG, "Bluetooth permission not granted.")
                    }
                }
        } else {
            Log.d(TAG, "BluetoothLeScanner is null")
        }
    }

    private fun bluetoothScanStop(bleScanCallback: BleScanCallback) {
        Log.d(TAG, "Stopping Bluetooth scan...")
        if (btScanner != null) {
            permissionManager
                .request(Permission.Bluetooth)
                .rationale("Bluetooth permission is needed")
                .checkPermission { isGranted ->
                    if (isGranted) {
                        Log.d(TAG, "Permissions granted, stop scan.")
                        btScanner.stopScan(bleScanCallback)
                    } else {
                        Log.d(TAG, "Bluetooth permission not granted.")
                    }
                }
        } else {
            Log.d(TAG, "BluetoothLeScanner is null")
        }

    }


    private fun showPermissionDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        if (alertDialog == null) {
            alertDialog = builder.create()
        }

        if (!alertDialog!!.isShowing) {
            alertDialog!!.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createBleScanCallback(): BleScanCallback {
        return BleScanCallback(
            onScanResultAction,
            onBatchScanResultAction,
            onScanFailedAction
        )
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private val onScanResultAction: (ScanResult?) -> Unit = { result ->
        Log.d(TAG, "onScanResultAction")

        val mac = result?.device
        val scanRecord = result?.scanRecord

//        if (scanRecord != null) {
//            Log.d(TAG, "Scan: ${scanRecord.getServiceData()}")
//        }
//
//        if (scanRecord != null) {
//            Log.d(TAG, "Scan: ${toString(scanRecord.getManufacturerSpecificData())}")
//        }

        val rssi = result?.rssi

        if (scanRecord != null) {
            scanRecord.bytes?.let {
                val parserBeacon = BeaconParser.parseIBeacon(it, mac, rssi)

                // Definir una lista de UUIDs permitidos
                val allowedUUIDs = listOf(
                    "2f234454cf6d4a0fadf2f4911ba9ffb6",
                    "2f234454cf6d4a0fadf2f4911ba9ffb7",
                    "2f234454cf6d4a0fadf2f4911ba9fff8"
                    // Añade más UUIDs aquí
                )

                // Verificar si el UUID del beacon está en la lista de UUIDs permitidos
                if (allowedUUIDs.contains(parserBeacon.uuid)) {
                    // Si el beacon no está ya en el mapa de beacons, lo agrega.
                    if (!beacons.containsKey(parserBeacon.uuid)) {
                        parserBeacon.uuid?.let { it1 -> beacons[it1] = parserBeacon }
                    }

                    // Recupera el beacon del mapa de beacons.
                    val beaconSave = beacons[parserBeacon.uuid]

                    if (beaconSave != null) {
                        // Actualiza el RSSI del beacon guardado.
                        beaconSave.rssi = parserBeacon.rssi

                        Log.d(TAG, "uuid ${beaconSave.uuid}")

                        // Calcula la distancia usando el txPower y el RSSI del beacon.
                        val distance = parserBeacon.txPower?.let { txPower ->
                            parserBeacon.rssi?.let { rssi ->
                                beaconSave.calculateDistance(txPower = txPower, rssi = rssi)
                            }
                        }
                        beaconSave.distance = distance?.toFloat()
                        Log.d(TAG, beaconSave.toString() + " distance en el beacon " + beaconSave.distance)

                        // Actualiza el LiveData _resultBeacons con los detalles del beacon y la distancia.
                        _resultBeacons.value = beaconSave.toString() + " distance " + distance

                        Log.d(TAG, beaconSave.uuid + " distance " + distance)

                        // Formatea la distancia a dos decimales.
                        val rounded_distance = String.format("%.2f", distance).toDouble()
                        when (allowedUUIDs.indexOf(parserBeacon.uuid)) {
                            0 -> txtMessage.text = beaconSave.toString() + " distance " + rounded_distance
                            1 -> txtMessage2.text = beaconSave.toString() + " distance " + rounded_distance
                            2 -> txtMessage3.text = beaconSave.toString() + " distance " + rounded_distance
                        }
                        val positions = arrayOf(
                            doubleArrayOf(240.0, 0.0),
                            doubleArrayOf(0.0, 0.0),
                            doubleArrayOf(120.0, 120.0)
                            )

                        val b1 = "2f234454cf6d4a0fadf2f4911ba9ffb6"
                        val b2 = "2f234454cf6d4a0fadf2f4911ba9ffb7"
                        val b3 = "2f234454cf6d4a0fadf2f4911ba9fff8"
                        if(beacons.get(b1)?.distance != null &&
                            beacons.get(b2)?.distance != null &&
                            beacons.get(b3)?.distance != null){
                            var distances= doubleArrayOf(
                                beacons.get(b1)?.distance!!.toDouble(),
                                beacons.get(b2)?.distance!!.toDouble(),
                                beacons.get(b3)?.distance!!.toDouble()
                            )
//                            Log.d("distances", beacons.get(b1)?.distance.toString() + "/" + beacons.get(b2)?.distance.toString() + "/  "/* beacons.get(b3)?.distance.toString()*/)
                            trilateration2DZeroDistance(positions, distances)
                        }


                        // Actualiza el mensaje de texto con los detalles del beacon y la distancia redondeada.
                        //txtMessage.text = beaconSave.toString() + " distance " + rounded_distance
                    }
                }
            }
        }
    }

    fun toString(array: SparseArray<ByteArray>?): String {
        if (array == null) {
            return "null"
        }
        if (array.size() == 0) {
            return "{}"
        }
        val buffer = java.lang.StringBuilder()
        buffer.append('{')
        for (i in 0 until array.size()) {
            buffer.append(array.keyAt(i)).append("=").append(array.valueAt(i).contentToString())
        }
        buffer.append('}')
        return buffer.toString()
    }
    private fun parseIBeaconUuidFromScanRecord(scanRecord: ScanRecord?): String? {
        scanRecord?.bytes?.let { bytes ->
            var startByte = 2
            while (startByte <= 5) {
                val uuidBytes = bytes.copyOfRange(startByte, startByte + 16)
                val uuid = bytesToHex(uuidBytes)
                // Verificar si es un paquete iBeacon por el patrón UUID
                if (uuid.length == 32) { // Verificar la longitud del UUID
                    return uuid
                }
                startByte += 16
            }
        }
        return null
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val hex = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val v = byte.toInt() and 0xFF
            hex.append(hexChars[v shr 4])
            hex.append(hexChars[v and 0x0F])
        }
        return hex.toString()
    }

    private fun parseAltBeaconUuidFromScanRecord(scanRecord: ScanRecord?): String? {
        scanRecord?.bytes?.let { bytes ->
            // AltBeacon tiene un formato fijo de 24 bytes para el UUID
            if (bytes.size >= 24) {
                val uuidBytes = bytes.copyOfRange(4, 20)
                val uuid = bytesToHex(uuidBytes)
                // Verificar si es un paquete AltBeacon por la longitud del UUID
                if (uuid.length == 32) { // Verificar la longitud del UUID
                    return uuid
                }
            }
        }
        return null
    }


    private val onBatchScanResultAction: (MutableList<ScanResult>?) -> Unit = {
        Log.d(TAG, "BatchScanResult: ${it.toString()}")
    }

    private val onScanFailedAction: (Int) -> Unit = {
        Log.d(TAG, "ScanFailed: $it")
    }

    fun getCoordinates(beaconsList : HashMap<String, Beacon>){
        beaconsList
    }
    fun trilateration2DZeroDistance(positions: Array<DoubleArray>, distance: DoubleArray){
        var trilaterationFunction = TrilaterationFunction(positions, distance)
        var lineal = LinearLeastSquaresSolver(trilaterationFunction)
        var nolineal = NonLinearLeastSquaresSolver(trilaterationFunction, LevenbergMarquardtOptimizer())

        var linealSolve = lineal.solve()
        var nolinealSolve =  nolineal.solve()

        var lineals = printDoubleArray(linealSolve.toArray())
        var nonlinea = printDoubleArray(nolinealSolve.getPoint().toArray())

//        Log.d("solutions", "linealSolve ${linealSolve}")
        Log.d("solutions", "no linealSolve ${nolinealSolve.point}")
    }


    private fun printDoubleArray(values: DoubleArray) : String {
        var output = ""
        for (p in values) {
            output = output + "p -- "
        }
        output = "\n"
        return output
    }
}
