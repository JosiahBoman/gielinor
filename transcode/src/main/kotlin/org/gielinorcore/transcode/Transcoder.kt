package org.gielinorcore.transcode

import org.gielinorcore.cache.model.Model
import org.gielinorcore.cache.model.ModelCodec

/** A revision family with its own binary formats. Add entries as revision modules land. */
enum class RevisionFormat { RS2_317, RS2_667, RS2_718, OSRS }

/**
 * Cross-revision asset conversion. This is what Elorin-style servers do by hand
 * with packer tools when they put OSRS bosses into a 667 cache. Here it is one
 * decode -> neutral -> encode step per asset type.
 *
 * DELTA T1: model codecs per revision.
 * DELTA T2: skeleton/frame (animation) codecs and the skin-id remap table.
 * DELTA T3: texture / sprite conversion (palette and dimension differences).
 * DELTA T4: map chunk conversion, including overlay/underlay id remaps.
 */
class Transcoder(private val modelCodecs: Map<RevisionFormat, ModelCodec>) {

    fun transcodeModel(bytes: ByteArray, from: RevisionFormat, to: RevisionFormat): ByteArray {
        val decoder = modelCodecs[from] ?: error("no model codec for $from (DELTA T1)")
        val encoder = modelCodecs[to] ?: error("no model codec for $to (DELTA T1)")
        val model: Model = decoder.decode(bytes)
        return encoder.encode(ModelRules.adapt(model, from, to))
    }
}

/** Per-pair adjustments that are not pure format differences (e.g. texture id spaces). */
object ModelRules {
    fun adapt(model: Model, from: RevisionFormat, to: RevisionFormat): Model {
        if (from == to) return model
        // DELTA T1: remap texture ids, clamp priorities, drop unsupported face types.
        return model
    }
}
