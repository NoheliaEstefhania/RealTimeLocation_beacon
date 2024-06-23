package com.idnp2024a.beaconscanner.BeaconScanerLibrary

object Utils {
    @OptIn(ExperimentalStdlibApi::class)
    fun toHexString(bytes:ByteArray):String{
        return bytes.toHexString()
    }
}