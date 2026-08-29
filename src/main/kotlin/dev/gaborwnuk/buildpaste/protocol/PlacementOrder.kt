package dev.gaborwnuk.buildpaste.protocol

/**
 * Maps a block's index in an uploaded build onto the world position it belongs at.
 *
 * Uploads walk the selected region in x, then y, then z, with the x and z sweeps
 * running forwards or backwards depending on which way the uploader was facing. Pasting
 * replays that same walk, re-anchored on the pasting player and turned to the direction
 * *they* are facing, so index `i` in the block list always lands on `positionAt(i)`.
 *
 * The mapping is computed per index rather than materialized as a list: a build may run
 * to millions of blocks, and holding a position object for each one is a great deal of
 * memory to carry for the length of a paste.
 */
class PlacementOrder(
	@PublishedApi internal val originX: Int,
	@PublishedApi internal val originY: Int,
	@PublishedApi internal val originZ: Int,
	@PublishedApi internal val sizeX: Int,
	@PublishedApi internal val sizeY: Int,
	@PublishedApi internal val sizeZ: Int,
	uploadDirection: String,
	pasteDirection: String,
) {
	/** Number of blocks the walk covers, as a Long so oversized builds can be rejected rather than wrap. */
	val count: Long = sizeX.toLong() * sizeY.toLong() * sizeZ.toLong()

	@PublishedApi internal val layer: Long = sizeY.toLong() * sizeZ.toLong()

	/**
	 * Which of the eight walks applies. Uploads facing north/south sweep the region in a
	 * different order from uploads facing east/west, and the paste direction picks one of
	 * four re-anchorings within that. A negative value means one of the directions was
	 * not a compass name, which the original treated as placing nothing.
	 */
	@PublishedApi internal val variant: Int = run {
		val uploadNorthSouth = uploadDirection == "north" || uploadDirection == "south"
		val facing = when (pasteDirection) {
			"north" -> 0
			"south" -> 1
			"east" -> 2
			"west" -> 3
			else -> -1
		}
		if (facing < 0) -1 else if (uploadNorthSouth) facing else facing + 4
	}

	/** True when both directions were understood; an unknown pair places nothing. */
	val isValid: Boolean get() = variant >= 0

	/**
	 * Calls [consume] with the world position of the block at [index].
	 *
	 * Inline, and passing loose coordinates rather than a position object, so that placing
	 * a multi-million block build allocates nothing per block.
	 */
	inline fun <T> positionAt(index: Int, consume: (x: Int, y: Int, z: Int) -> T): T {
		val i = index.toLong()
		val xi = (i / layer).toInt()
		val remainder = i % layer
		val yi = (remainder / sizeZ).toInt()
		val zi = (remainder % sizeZ).toInt()

		// A descending sweep visits sizeN, sizeN-1, ... 1; an ascending one visits 0 ... sizeN-1.
		val xDown = sizeX - xi
		val xUp = xi
		val zDown = sizeZ - zi
		val zUp = zi

		var x = originX
		var z = originZ
		when (variant) {
			0 -> { x = originX + xDown; z = originZ + zDown - sizeZ }        // upload N/S, paste north
			1 -> { x = originX + xUp - sizeX; z = originZ + zUp }            // upload N/S, paste south
			2 -> { x = originX + zUp; z = originZ + xDown }                  // upload N/S, paste east
			3 -> { x = originX + zDown - sizeZ; z = originZ + xUp - sizeX }  // upload N/S, paste west
			4 -> { x = originX + zDown; z = originZ + xDown - sizeX }        // upload E/W, paste north
			5 -> { x = originX + zUp - sizeZ; z = originZ + xUp }            // upload E/W, paste south
			6 -> { x = originX + xUp; z = originZ + zDown }                  // upload E/W, paste east
			7 -> { x = originX + xDown - sizeX; z = originZ + zUp - sizeZ }  // upload E/W, paste west
		}

		return consume(x, originY + yi, z)
	}
}
