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
                    // Short timeout so the loop can respond to cancellation promptly.
                    // The heartbeat coroutine below is the sole keepalive mechanism.
                    socket.soTimeout = 1000

                    // Heartbeat: send "A" every 100 ms as required by the GT7 protocol.
                    val heartbeatJob = launch {
                        while (isActive) {
                            sendHeartbeat()
                            delay(100)
                        }
                    }

                    val buffer = ByteArray(4096)
                    var connected = false

                    try {
                        while (isActive) {
                            try {
                                val packet = DatagramPacket(buffer, buffer.size)
                                socket.receive(packet)

                                // Protocol spec: minimum 68 bytes required for IV extraction.
                                if (packet.length < 68) continue

                                val rawData = packet.data.copyOf(packet.length)
                                val decoded = decodeSalsa20(rawData)

                                if (decoded.isNotEmpty()) {
                                    if (!connected) {
                                        connected = true
                                        val fromIp = packet.address.hostAddress ?: ip
                                        withContext(Dispatchers.Main) { onConnected(fromIp) }
                                    }

                                    // Offset 0x3C = RPM (see packet_structure.md)
                                    val rpm = parsePacket(decoded)
                                    packetCount++
                                    withContext(Dispatchers.Main) {
                                        onPacketReceived(packetCount, rpm)
                                    }
                                }
                            } catch (e: SocketTimeoutException) {
                                // Expected when no packet arrives within soTimeout — keep looping.
                            }
                        }
                    } finally {
                        heartbeatJob.cancel()
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

    internal fun decodeSalsa20(dat: ByteArray): ByteArray {
        try {
            val key = "Simulator Interface Packet GT7 ver 0.0".toByteArray().copyOf(32)

            // IV is derived from bytes [64:68] of the encrypted packet (gt7_protocol_reference.md §3.2)
            val iv1 = ByteBuffer.wrap(dat, 0x40, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val iv2 = iv1 xor 0xDEADBEAF.toInt()   // note: BEAF not BEEF

            val iv = ByteArray(8)
            ByteBuffer.wrap(iv).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(iv2)   // [0:4] iv2 LE
                putInt(iv1)   // [4:8] iv1 LE
            }

            val cipher = Salsa20Engine()
            cipher.init(false, ParametersWithIV(KeyParameter(key), iv))

            val decrypted = ByteArray(dat.size)
            cipher.processBytes(dat, 0, dat.size, decrypted, 0)

            // Magic 0x47375330 ("GT70") must be at offset 0x00 of the decrypted packet.
            val magic = ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            return if (magic == 0x47375330) decrypted else ByteArray(0)
        } catch (e: Exception) {
            Log.e("GT7UdpClient", "Decryption error: ${e.message}", e)
            return ByteArray(0)
        }
    }

    internal fun parsePacket(decoded: ByteArray): Float {
        return if (decoded.size >= 0x3C + 4) {
            ByteBuffer.wrap(decoded, 0x3C, 4).order(ByteOrder.LITTLE_ENDIAN).float
        } else 0f
    }
}
