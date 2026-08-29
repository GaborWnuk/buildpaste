package dev.gaborwnuk.buildpaste.protocol

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins [PlacementOrder] to the walk the Bukkit plugin performed.
 *
 * The reference below is a direct transcription of the original nested loops. Builds
 * uploaded by the old plugin are still on the backend, so any divergence here would
 * paste them scrambled.
 */
class PlacementOrderTest {

	private fun reference(
		originX: Int, originY: Int, originZ: Int,
		sizeX: Int, sizeY: Int, sizeZ: Int,
		uploadDirection: String, direction: String,
	): List<Triple<Int, Int, Int>> {
		val result = mutableListOf<Triple<Int, Int, Int>>()
		val uploadNS = uploadDirection == "north" || uploadDirection == "south"

		if (uploadNS) {
			when (direction) {
				"north" -> for (x in sizeX downTo 1) for (y in 0 until sizeY) for (z in sizeZ downTo 1)
					result.add(Triple(originX + x, originY + y, originZ + z - sizeZ))
				"south" -> for (x in 0 until sizeX) for (y in 0 until sizeY) for (z in 0 until sizeZ)
					result.add(Triple(originX + x - sizeX, originY + y, originZ + z))
				"east" -> for (x in sizeX downTo 1) for (y in 0 until sizeY) for (z in 0 until sizeZ)
					result.add(Triple(originX + z, originY + y, originZ + x))
				"west" -> for (x in 0 until sizeX) for (y in 0 until sizeY) for (z in sizeZ downTo 1)
					result.add(Triple(originX + z - sizeZ, originY + y, originZ + x - sizeX))
			}
		} else {
			when (direction) {
				"north" -> for (x in sizeX downTo 1) for (y in 0 until sizeY) for (z in sizeZ downTo 1)
					result.add(Triple(originX + z, originY + y, originZ + x - sizeX))
				"south" -> for (x in 0 until sizeX) for (y in 0 until sizeY) for (z in 0 until sizeZ)
					result.add(Triple(originX + z - sizeZ, originY + y, originZ + x))
				"east" -> for (x in 0 until sizeX) for (y in 0 until sizeY) for (z in sizeZ downTo 1)
					result.add(Triple(originX + x, originY + y, originZ + z))
				"west" -> for (x in sizeX downTo 1) for (y in 0 until sizeY) for (z in 0 until sizeZ)
					result.add(Triple(originX + x - sizeX, originY + y, originZ + z - sizeZ))
			}
		}
		return result
	}

	private fun actual(
		originX: Int, originY: Int, originZ: Int,
		sizeX: Int, sizeY: Int, sizeZ: Int,
		uploadDirection: String, direction: String,
	): List<Triple<Int, Int, Int>> {
		val order = PlacementOrder(originX, originY, originZ, sizeX, sizeY, sizeZ, uploadDirection, direction)
		return (0 until order.count.toInt()).map { i ->
			order.positionAt(i) { x, y, z -> Triple(x, y, z) }
		}
	}

	@Test
	fun `matches the original walk for every upload and paste direction`() {
		val directions = listOf("north", "east", "south", "west")
		for (upload in directions) {
			for (paste in directions) {
				assertEquals(
					reference(10, 64, -30, 3, 4, 5, upload, paste),
					actual(10, 64, -30, 3, 4, 5, upload, paste),
					"upload=$upload paste=$paste",
				)
			}
		}
	}

	@Test
	fun `matches the original walk for randomly shaped builds`() {
		val random = Random(20260829)
		val directions = listOf("north", "east", "south", "west")
		repeat(200) {
			val ox = random.nextInt(-5000, 5000)
			val oy = random.nextInt(-64, 320)
			val oz = random.nextInt(-5000, 5000)
			val sx = random.nextInt(1, 7)
			val sy = random.nextInt(1, 7)
			val sz = random.nextInt(1, 7)
			val upload = directions.random(random)
			val paste = directions.random(random)
			assertEquals(
				reference(ox, oy, oz, sx, sy, sz, upload, paste),
				actual(ox, oy, oz, sx, sy, sz, upload, paste),
				"origin=($ox,$oy,$oz) size=($sx,$sy,$sz) upload=$upload paste=$paste",
			)
		}
	}

	@Test
	fun `covers every block of the region exactly once`() {
		val order = PlacementOrder(0, 0, 0, 4, 3, 5, "south", "east")
		val seen = (0 until order.count.toInt()).map { i -> order.positionAt(i) { x, y, z -> Triple(x, y, z) } }
		assertEquals(4 * 3 * 5, seen.size)
		assertEquals(seen.size, seen.toSet().size, "walk must not revisit a position")
	}

	@Test
	fun `reports an unknown direction pair as invalid`() {
		assertFalse(PlacementOrder(0, 0, 0, 2, 2, 2, "south", "sideways").isValid)
		assertTrue(PlacementOrder(0, 0, 0, 2, 2, 2, "south", "east").isValid)
	}

	@Test
	fun `survives coordinates far from the origin`() {
		val order = PlacementOrder(-29_999_000, -64, 29_999_000, 2, 2, 2, "south", "north")
		assertEquals(
			reference(-29_999_000, -64, 29_999_000, 2, 2, 2, "south", "north"),
			(0 until order.count.toInt()).map { i -> order.positionAt(i) { x, y, z -> Triple(x, y, z) } },
		)
	}
}
