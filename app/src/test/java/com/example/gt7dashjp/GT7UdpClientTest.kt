package com.example.gt7dashjp

import com.example.gt7dashjp.data.GT7UdpClient
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GT7UdpClientTest {
    private val client = GT7UdpClient(
        onConnected = {},
        onPacketReceived = { _, _ -> },
        onError = {},
        onStatus = {}
    )

    @Test
    fun decodeSalsa20_returnsExpectedPacket() {
        val expectedRpm = 8450.25f
        val encrypted = buildEncryptedPacket(expectedRpm)

        val decrypted = client.decodeSalsa20(encrypted)

        assertTrue(decrypted.isNotEmpty())
        assertEquals(0x47375330, ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        assertEquals(expectedRpm, ByteBuffer.wrap(decrypted, 0x3C, 4).order(ByteOrder.LITTLE_ENDIAN).float, 0.0001f)
    }

    @Test
    fun parsePacket_extractsRpmFromDecodedPacket() {
        val expectedRpm = 1234.5f
        val decrypted = buildDecodedPacket(expectedRpm)

        val parsedRpm = client.parsePacket(decrypted)

        assertEquals(expectedRpm, parsedRpm, 0.0001f)
    }

    private fun buildEncryptedPacket(rpm: Float): ByteArray {
        val plaintext = buildDecodedPacket(rpm)
        val ivSource = ByteArray(4)
        ByteBuffer.wrap(ivSource).order(ByteOrder.LITTLE_ENDIAN).putInt(0x12345678)
        val iv1 = ByteBuffer.wrap(ivSource).order(ByteOrder.LITTLE_ENDIAN).int
        val iv2 = iv1 xor 0xDEADBEAF.toInt()

        val iv = ByteArray(8)
        ByteBuffer.wrap(iv).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(iv2)
            putInt(iv1)
        }

        val cipher = Salsa20Engine()
        cipher.init(false, ParametersWithIV(KeyParameter(KEY), iv))

        val encrypted = ByteArray(plaintext.size)
        cipher.processBytes(plaintext, 0, plaintext.size, encrypted, 0)
        encrypted[0x40] = ivSource[0]
        encrypted[0x41] = ivSource[1]
        encrypted[0x42] = ivSource[2]
        encrypted[0x43] = ivSource[3]

        return encrypted
    }

    private fun buildDecodedPacket(rpm: Float): ByteArray {
        val decoded = ByteArray(0x80)
        ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN).putInt(0x47375330)
        ByteBuffer.wrap(decoded, 0x3C, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(rpm)
        return decoded
    }

    companion object {
        private val KEY = "Simulator Interface Packet GT7 ver 0.0".toByteArray().copyOf(32)
    }
}
