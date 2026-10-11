package org.gielinorcore.server

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.gielinorcore.game.World

/**
 * First byte of a connection selects the service: game login, Js5 file serving,
 * or the world list. Selecting a [org.gielinorcore.protocol.Revision] and
 * answering each service is DELTA P2 (login) and DELTA P4 (Js5 server).
 */
class HandshakeHandler(private val world: World) : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (!input.isReadable) return
        val service = input.readUnsignedByte().toInt()
        when (service) {
            SERVICE_GAME -> TODO("DELTA P2: login handshake for the configured revision")
            SERVICE_JS5 -> TODO("DELTA P4: serve cache groups to the client")
            else -> ctx.close()
        }
    }

    companion object {
        const val SERVICE_GAME = 14
        const val SERVICE_JS5 = 15
    }
}
