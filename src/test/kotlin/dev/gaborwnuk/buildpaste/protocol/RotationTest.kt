package dev.gaborwnuk.buildpaste.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RotationTest {

	private val compass = listOf("north", "east", "south", "west")

	@Test
	fun `look direction follows the games yaw convention`() {
		assertEquals("south", Rotation.lookDirection(0f))
		assertEquals("west", Rotation.lookDirection(90f))
		assertEquals("north", Rotation.lookDirection(180f))
		assertEquals("east", Rotation.lookDirection(270f))
		assertEquals("east", Rotation.lookDirection(-90f))
		assertEquals("south", Rotation.lookDirection(359f))
	}

	@Test
	fun `look direction normalizes yaw that has wound past a full turn`() {
		// The original added 360 only once, so a yaw of 720 fell through as north.
		assertEquals("south", Rotation.lookDirection(720f))
		assertEquals("west", Rotation.lookDirection(450f))
		assertEquals("east", Rotation.lookDirection(-450f))
	}

	@Test
	fun `a build pasted in the direction it was uploaded is unchanged`() {
		for (direction in compass) {
			assertEquals(
				"[facing=north,half=top]",
				Rotation.rotateProperties("[facing=north,half=top]", direction, direction),
			)
		}
	}

	@Test
	fun `facing turns with the quarter turn between upload and paste`() {
		assertEquals("[facing=west]", Rotation.rotateProperties("[facing=north]", "south", "east"))
		assertEquals("[facing=south]", Rotation.rotateProperties("[facing=north]", "north", "south"))
		assertEquals("[facing=west]", Rotation.rotateProperties("[facing=north]", "east", "north"))
	}

	@Test
	fun `vertical facing is left alone`() {
		assertEquals("[facing=up]", Rotation.rotateProperties("[facing=up]", "north", "east"))
		assertEquals("[facing=down]", Rotation.rotateProperties("[facing=down]", "north", "west"))
	}

	@Test
	fun `fence connections rotate as a set rather than one at a time`() {
		// north+east connected, turned a quarter clockwise, becomes east+south.
		val rotated = Rotation.rotateProperties(
			"[north=true,east=true,south=false,west=false,waterlogged=false]",
			"north",
			"east",
		)
		val props = parse(rotated!!)
		assertEquals("false", props["north"])
		assertEquals("true", props["east"])
		assertEquals("true", props["south"])
		assertEquals("false", props["west"])
		assertEquals("false", props["waterlogged"])
	}

	@Test
	fun `wall heights rotate with their side`() {
		val props = parse(Rotation.rotateProperties("[north=low,east=tall,up=true]", "north", "south")!!)
		assertEquals("low", props["south"])
		assertEquals("tall", props["west"])
		assertEquals("true", props["up"])
	}

	@Test
	fun `pillar axis swaps only on a quarter turn`() {
		assertEquals("[axis=z]", Rotation.rotateProperties("[axis=x]", "north", "east"))
		assertEquals("[axis=x]", Rotation.rotateProperties("[axis=x]", "north", "south"))
		assertEquals("[axis=y]", Rotation.rotateProperties("[axis=y]", "north", "east"))
	}

	@Test
	fun `values that are not property strings pass through`() {
		assertNull(Rotation.rotateProperties(null, "north", "east"))
		assertEquals("", Rotation.rotateProperties("", "north", "east"))
		assertEquals("[]", Rotation.rotateProperties("[]", "north", "east"))
		assertEquals("not data", Rotation.rotateProperties("not data", "north", "east"))
	}

	@Test
	fun `four quarter turns return a state to itself`() {
		val start = "[facing=north,north=true,west=true,axis=x]"
		var state = start
		repeat(4) { state = Rotation.rotateProperties(state, "north", "east")!! }

		// Rotating a partial set of connection flags writes back all four sides, so the
		// result carries explicit `east=false` and `south=false` that the input left out.
		// Every property the input did state must have come back to its original value.
		val before = parse(start)
		val after = parse(state)
		for ((key, value) in before) assertEquals(value, after[key], "property $key after a full turn")
		for ((key, value) in after) if (key !in before) assertEquals("false", value, "property $key filled in by rotation")
	}

	private fun parse(data: String): Map<String, String> =
		data.removePrefix("[").removeSuffix("]")
			.split(',')
			.filter { it.isNotBlank() }
			.associate { it.substringBefore('=') to it.substringAfter('=') }
}
