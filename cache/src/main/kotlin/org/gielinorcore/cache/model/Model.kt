package org.gielinorcore.cache.model

/**
 * Revision-neutral mesh. Every revision's model format decodes into this and
 * encodes out of it, which is what makes cross-revision transcoding a pure
 * data problem (see :transcode).
 */
data class Model(
    val vertexX: IntArray,
    val vertexY: IntArray,
    val vertexZ: IntArray,
    val faceA: IntArray,
    val faceB: IntArray,
    val faceC: IntArray,
    val faceColor: IntArray,
    val faceType: IntArray? = null,
    val facePriority: IntArray? = null,
    val faceAlpha: IntArray? = null,
    val faceTexture: IntArray? = null,
    /** Vertex skin groups drive skeletal animation; null when the model is static. */
    val vertexSkins: IntArray? = null,
    val faceSkins: IntArray? = null,
) {
    val vertexCount get() = vertexX.size
    val faceCount get() = faceA.size
}

interface ModelCodec {
    fun decode(bytes: ByteArray): Model
    fun encode(model: Model): ByteArray
}
