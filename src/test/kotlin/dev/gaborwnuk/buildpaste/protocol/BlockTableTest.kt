package dev.gaborwnuk.buildpaste.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockTableTest {

	@Test
	fun `the table keeps the size and order the backend indexes by`() {
		assertEquals(1140, BlockTable.names.size)
		assertEquals("air", BlockTable.names.first())
		assertEquals("stone", BlockTable.names[1])
		assertEquals("warped_shelf", BlockTable.names.last())
	}

	@Test
	fun `every id resolves to a name that maps back to an id for the same block`() {
		for (id in BlockTable.names.indices) {
			val name = BlockTable.nameOf(id) ?: error("no name for id $id")
			// A renamed block resolves to its current name, which is deliberately not in the
			// table; the rest must map back to an id naming the same block. Two names appear
			// in the table twice, so the id that comes back is not always the one we started
			// from — what matters is that it denotes the same block.
			if (name.removePrefix("minecraft:") != BlockTable.names[id]) continue
			val back = BlockTable.idOf(name) ?: error("$name did not map back to an id")
			assertEquals(BlockTable.names[id], BlockTable.names[back])
		}
	}

	@Test
	fun `the two names the table lists twice both resolve to the same block`() {
		assertEquals("minecraft:melon_stem", BlockTable.nameOf(492))
		assertEquals("minecraft:melon_stem", BlockTable.nameOf(533))
		assertEquals("minecraft:pumpkin_stem", BlockTable.nameOf(493))
		assertEquals("minecraft:pumpkin_stem", BlockTable.nameOf(532))
	}

	@Test
	fun `blocks the game renamed resolve to their current name`() {
		assertEquals("minecraft:short_grass", BlockTable.nameOf(BlockTable.idOf("grass")!!))
		assertEquals("minecraft:dirt_path", BlockTable.nameOf(BlockTable.idOf("grass_path")!!))
		assertEquals("minecraft:oak_sign", BlockTable.nameOf(BlockTable.idOf("sign")!!))
		assertEquals("minecraft:oak_wall_sign", BlockTable.nameOf(BlockTable.idOf("wall_sign")!!))
		assertEquals("minecraft:iron_chain", BlockTable.nameOf(BlockTable.idOf("chain")!!))
	}

	@Test
	fun `resolve accepts the numeric and named forms the backend sends`() {
		// Gson hands integers back as floating point, which is how the plugin received them.
		assertEquals("minecraft:stone", BlockTable.resolve("1"))
		assertEquals("minecraft:stone", BlockTable.resolve("1.0"))
		// A block outside the table travels as its own name.
		assertEquals("minecraft:candle_cake", BlockTable.resolve("minecraft:candle_cake"))
	}

	@Test
	fun `unknown names and out of range ids report themselves`() {
		assertNull(BlockTable.idOf("minecraft:not_a_block"))
		assertNull(BlockTable.nameOf(99_999))
	}
}
