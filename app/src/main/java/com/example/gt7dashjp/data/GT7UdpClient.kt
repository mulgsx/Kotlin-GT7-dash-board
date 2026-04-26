package com.example.gt7dashjp.data

import android.util.Log
import kotlinx.coroutines.*
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GT7UdpClient(
    private val onConnected: (fromIp: String) -> Unit,
    private val onPacketReceived: (packetCount: Int, rpm: Float) -> Unit,
    private val onError: (message: String) -> Unit,
    private val onStatus: (message: String) -> Unit
) {
    private val sendPort = 33739
    private val receivePort = 33740
    private var packetCount = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var targetIp: String = ""

    fun start(ip: String) {
        targetIp = ip
        packetCount = 0
        job = scope.launch {
            try {
                DatagramSocket(receivePort).use { socket ->
                    socket.soTimeout = 5000
                    sendHeartbeat()

                    val buffer = ByteArray(4096)
                    var connected = false
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)

                            val rawData = packet.data.copyOf(packet.length)
                            val decoded = decodeSalsa20(rawData)

                            if (decoded.isNotEmpty()) {
                                if (!connected) {
                                    connected = true
                                    val fromIp = packet.address.hostAddress ?: ip
                                    withContext(Dispatchers.Main) { onConnected(fromIp) }
                                }

                                val rpm = getFloat(decoded, 0x1C)
                                packetCount++
                                withContext(Dispatchers.Main) { onPacketReceived(packetCount, rpm) }

                                if (packetCount % 100 == 0) sendHeartbeat()
                            }

                        } catch (e: SocketTimeoutException) {
                            sendHeartbeat()
                            withContext(Dispatchers.Main) { onStatus("Waiting for data...") }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GT7UdpClient", "Communication error: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun stopCommunication() {
        job?.cancel()
        job = null
    }

    private fun sendHeartbeat() {
        try {
            DatagramSocket().use { sendSocket ->
                val data = "A".toByteArray()
                val address = InetAddress.getByName(targetIp)
                val packet = DatagramPacket(data, data.size, address, sendPort)
                sendSocket.send(packet)
            }
        } catch (e: Exception) {
            Log.e("GT7UdpClient", "Heartbeat send error: ${e.message}", e)
        }
    }

    private fun decodeSalsa20(dat: ByteArray): ByteArray {
        try {
            val key = "Simulator Interface Packet GT7 ver 0.0".toByteArray().copyOf(32)
            val oiv = dat.copyOfRange(0x40, 0x44)
            val iv1 = ByteBuffer.wrap(oiv).order(ByteOrder.LITTLE_ENDIAN).int
            val iv2 = iv1 xor 0xDEADBEAF.toInt()

            val iv = ByteArray(8)
            ByteBuffer.wrap(iv).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(iv2)
                putInt(iv1)
            }

            val cipher = Salsa20Engine()
            cipher.init(false, ParametersWithIV(KeyParameter(key), iv))

            val decrypted = ByteArray(dat.size)
            cipher.processBytes(dat, 0, dat.size, decrypted, 0)

            val magic = ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            return if (magic == 0x47375330) decrypted else ByteArray(0)
        } catch (e: Exception) {
            Log.e("GT7UdpClient", "Decryption error: ${e.message}", e)
            return ByteArray(0)
        }
    }

    private fun getFloat(decoded: ByteArray, offset: Int): Float {
        return if (decoded.size >= offset + 4) {
            ByteBuffer.wrap(decoded, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
        } else 0f
    }
}
