package org.gielinorcore.buffer

import kotlin.test.Test
import kotlin.test.assertEquals

class BufferRoundTripTest {

    @Test
    fun `integers and smarts round trip`() {
        val bytes = WriteBuffer()
            .u8(0xAB).u16(0xCDEF).u24(0x123456).i32(-7).i64(Long.MIN_VALUE + 5)
            .smart(5).smart(300).signedSmart(-100).signedSmart(10).bigSmart(70_000)
            .string("héllo").versionedString("x")
            .toByteArray()

        val r = ReadBuffer(bytes)
        assertEquals(0xAB, r.u8())
        assertEquals(0xCDEF, r.u16())
        assertEquals(0x123456, r.u24())
        assertEquals(-7, r.i32())
        assertEquals(Long.MIN_VALUE + 5, r.i64())
        assertEquals(5, r.smart())
        assertEquals(300, r.smart())
        assertEquals(-100, r.signedSmart())
        assertEquals(10, r.signedSmart())
        assertEquals(70_000, r.bigSmart())
        assertEquals("héllo", r.string())
        assertEquals("x", r.versionedString())
        assertEquals(0, r.remaining)
    }

    @Test
    fun `bit packing round trips`() {
        val out = WriteBuffer()
        BitWriter(out).bits(1, 1).bits(2, 3).bits(5, 17).bits(11, 1500).bits(30, 123_456_789).finish()
        val r = BitReader(ReadBuffer(out.toByteArray()))
        assertEquals(1, r.bits(1))
        assertEquals(3, r.bits(2))
        assertEquals(17, r.bits(5))
        assertEquals(1500, r.bits(11))
        assertEquals(123_456_789, r.bits(30))
    }
}
