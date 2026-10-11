package org.gielinorcore.protocol

import org.gielinorcore.buffer.ReadBuffer
import org.gielinorcore.buffer.WriteBuffer

/** A client build the server can speak to. One implementation per supported revision (DELTA P1). */
interface Revision {
    val id: Int
    val incoming: PacketRegistry<IncomingMessage>
    val outgoing: PacketRegistry<OutgoingMessage>
}

/** Marker types so game code never touches opcodes or byte layouts. */
interface IncomingMessage
interface OutgoingMessage

sealed interface PacketLength {
    data class Fixed(val size: Int) : PacketLength
    data object VariableByte : PacketLength
    data object VariableShort : PacketLength
}

interface IncomingCodec<M : IncomingMessage> {
    fun decode(buffer: ReadBuffer): M
}

interface OutgoingCodec<M : OutgoingMessage> {
    fun encode(message: M, out: WriteBuffer)
}

data class PacketSpec<M>(val opcode: Int, val length: PacketLength, val type: Class<out M>, val codec: Any)

/** Opcode <-> message mapping for one direction of one revision. */
class PacketRegistry<M : Any> {
    private val byOpcode = HashMap<Int, PacketSpec<M>>()
    private val byType = HashMap<Class<out M>, PacketSpec<M>>()

    fun register(spec: PacketSpec<M>) {
        require(byOpcode.put(spec.opcode, spec) == null) { "duplicate opcode ${spec.opcode}" }
        require(byType.put(spec.type, spec) == null) { "duplicate message type ${spec.type.simpleName}" }
    }

    fun byOpcode(opcode: Int): PacketSpec<M>? = byOpcode[opcode]
    fun byType(type: Class<out M>): PacketSpec<M>? = byType[type]
    val size get() = byOpcode.size
}

/** Login handshake stages are also per revision, but the states are shared (DELTA P2). */
enum class LoginStage { HANDSHAKE, KEY_EXCHANGE, CREDENTIALS, COMPLETE }
