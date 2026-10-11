package org.gielinorcore.content

import kotlinx.serialization.Serializable

/**
 * Partial definitions. Every field is nullable so a pack only states what it
 * changes. [OverlayCompiler] merges them over the decoded base definition.
 */
@Serializable
data class ItemOverride(
    val name: String? = null,
    val modelId: Int? = null,
    val stackable: Boolean? = null,
    val value: Int? = null,
    val inventoryOptions: List<String?>? = null,
    val groundOptions: List<String?>? = null,
    val recolorFrom: List<Int>? = null,
    val recolorTo: List<Int>? = null,
    val params: Map<Int, String>? = null,
)

@Serializable
data class NpcOverride(
    val name: String? = null,
    val models: List<Int>? = null,
    val size: Int? = null,
    val standAnimation: Int? = null,
    val walkAnimation: Int? = null,
    val combatLevel: Int? = null,
    val options: List<String?>? = null,
)

@Serializable
data class ObjectOverride(
    val name: String? = null,
    val models: List<Int>? = null,
    val sizeX: Int? = null,
    val sizeY: Int? = null,
    val solid: Boolean? = null,
    val options: List<String?>? = null,
)
