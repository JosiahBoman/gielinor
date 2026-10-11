package org.gielinorcore.protocol

import org.gielinorcore.buffer.ReadBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PacketRegistryTest {
    data class Ping(val nonce: Int) : IncomingMessage

    private val codec = object : IncomingCodec<Ping> { override fun decode(buffer: ReadBuffer) = Ping(buffer.i32()) }

    @Test
    fun `registry maps both ways and rejects duplicates`() {
        val reg = PacketRegistry<IncomingMessage>()
        reg.register(PacketSpec(0, PacketLength.Fixed(4), Ping::class.java, codec))
        assertEquals(Ping::class.java, reg.byOpcode(0)!!.type)
        assertEquals(0, reg.byType(Ping::class.java)!!.opcode)
        assertFailsWith<IllegalArgumentException> { reg.register(PacketSpec(0, PacketLength.VariableByte, Ping::class.java, codec)) }
    }
}
