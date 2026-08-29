package dev.gaborwnuk.buildpaste.protocol

/**
 * Compass-direction maths for re-orienting a build between the direction it was
 * uploaded facing and the direction the player is facing when pasting.
 *
 * Directions travel over the wire as the lowercase names `north`, `east`, `south`
 * and `west`, so they are handled as strings rather than as `Direction` values.
 */
object Rotation {

	private val COMPASS = listOf("north", "east", "south", "west")

	fun facingToDegrees(facing: String): Int = when (facing) {
		"north" -> 0
		"east" -> 90
		"south" -> 180
		"west" -> 270
		else -> 0
	}

	fun degreesToFacing(degrees: Int): String = when (degrees) {
		90 -> "east"
		180 -> "south"
		270 -> "west"
		else -> "north"
	}

	/** The direction a player with this yaw is looking, in the game's `yaw 0 == south` convention. */
	fun lookDirection(yaw: Float): String {
		val normalized = ((yaw % 360f) + 360f) % 360f
		return when {
			normalized >= 315f || normalized < 45f -> "south"
			normalized < 135f -> "west"
			normalized < 225f -> "north"
			else -> "east"
		}
	}

	/** Rotates a single compass value; non-compass values (`up`, `down`) pass through. */
	fun rotateFacing(blockDir: String, pasteDir: String, uploadDir: String): String {
		if (blockDir !in COMPASS) return blockDir
		val diff = facingToDegrees(pasteDir) - facingToDegrees(uploadDir)
		return degreesToFacing((360 + facingToDegrees(blockDir) + diff) % 360)
	}

	/**
	 * Rotates a serialized block-state property string such as
	 * `[facing=north,half=top,waterlogged=false]` from the build's upload direction to
	 * the paste direction.
	 *
	 * Three families of property need turning: `facing`, the per-side connection flags
	 * that fences, walls and panes use (`north=true`, or `north=low` for wall heights),
	 * and the `axis` of pillar-like blocks, which only swaps on a quarter turn.
	 */
	fun rotateProperties(data: String?, uploadDirection: String, pasteDirection: String): String? {
		if (data.isNullOrEmpty()) return data
		if (!data.startsWith("[") || !data.endsWith("]")) return data

		val inner = data.substring(1, data.length - 1).trim()
		if (inner.isEmpty()) return data

		val props = LinkedHashMap<String, String>()
		for (part in inner.split(',')) {
			val trimmed = part.trim()
			if (trimmed.isEmpty()) continue
			val eq = trimmed.indexOf('=')
			if (eq > 0) props[trimmed.substring(0, eq)] = trimmed.substring(eq + 1)
		}

		val diff = (360 + facingToDegrees(pasteDirection) - facingToDegrees(uploadDirection)) % 360

		props["facing"]?.let { props["facing"] = rotateFacing(it, pasteDirection, uploadDirection) }

		// Pull the per-side connection properties out so they can be re-keyed as a set;
		// rotating them one at a time in place would let an earlier write be overwritten
		// by a later one that rotates onto the same side.
		val boolConnections = LinkedHashMap<String, Boolean>()
		val heightConnections = LinkedHashMap<String, String>()
		for (dir in COMPASS) {
			val value = props[dir]?.lowercase() ?: continue
			when (value) {
				"true", "false" -> {
					boolConnections[dir] = value.toBoolean()
					props.remove(dir)
				}
				"none", "low", "tall" -> {
					heightConnections[dir] = value
					props.remove(dir)
				}
			}
		}

		if (boolConnections.isNotEmpty()) {
			if (diff == 0) {
				boolConnections.forEach { (dir, connected) -> props[dir] = connected.toString() }
			} else {
				val rotated = COMPASS.associateWithTo(LinkedHashMap()) { false }
				boolConnections.filterValues { it }.keys.forEach { dir ->
					rotated[degreesToFacing((360 + facingToDegrees(dir) + diff) % 360)] = true
				}
				rotated.forEach { (dir, connected) -> props[dir] = connected.toString() }
			}
		}

		if (heightConnections.isNotEmpty()) {
			if (diff == 0) {
				heightConnections.forEach { (dir, height) -> props[dir] = height }
			} else {
				val rotated = LinkedHashMap<String, String>()
				heightConnections.forEach { (dir, height) ->
					rotated[degreesToFacing((360 + facingToDegrees(dir) + diff) % 360)] = height
				}
				rotated.forEach { (dir, height) -> props[dir] = height }
			}
		}

		props["axis"]?.let { axis ->
			val lower = axis.lowercase()
			props["axis"] = if (lower != "y" && diff % 180 != 0) {
				when (lower) {
					"x" -> "z"
					"z" -> "x"
					else -> lower
				}
			} else {
				lower
			}
		}

		return props.entries.joinToString(",", prefix = "[", postfix = "]") { "${it.key}=${it.value}" }
	}
}
