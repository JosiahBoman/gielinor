package org.gielinorcore.cache.map

/** 64x64x4 terrain + object placement for one region (DELTA C5 for codecs). */
data class MapRegion(
    val regionX: Int,
    val regionY: Int,
    val tileHeights: Array<Array<IntArray>>,
    val overlayIds: Array<Array<IntArray>>,
    val underlayIds: Array<Array<IntArray>>,
    val tileFlags: Array<Array<IntArray>>,
    val objects: List<PlacedObject>,
) {
    data class PlacedObject(val id: Int, val x: Int, val y: Int, val plane: Int, val type: Int, val rotation: Int)

    companion object {
        const val SIZE = 64
        const val PLANES = 4
    }
}
