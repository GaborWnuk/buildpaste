package dev.gaborwnuk.buildpaste.paste

/**
 * Maps a block to the item a player needs in order to place it.
 *
 * `/construct` bills a build against the player's inventory, and for many blocks the two
 * names differ: wheat grows from seeds, a wall torch is placed from a torch, and every
 * potted plant is really a flower pot.
 */
object BlockItemMapping {

	private val EXPLICIT = mapOf(
		"attached_melon_stem" to "melon_seeds",
		"attached_pumpkin_stem" to "pumpkin_seeds",
		"melon_stem" to "melon_seeds",
		"pumpkin_stem" to "pumpkin_seeds",
		"bamboo_sapling" to "bamboo",
		"chorus_plant" to "chorus_fruit",
		"cocoa" to "cocoa_beans",
		"farmland" to "dirt",
		"frosted_ice" to "ice",
		"grass_path" to "grass",
		"kelp_plant" to "kelp",
		"moving_piston" to "piston",
		"redstone_wire" to "redstone",
		"snow" to "snowball",
		"sweet_berry_bush" to "sweet_berries",
		"beetroots" to "beetroot",
		"potatoes" to "potato",
		"carrots" to "carrot",
		"wheat" to "wheat_seeds",
	).mapKeys { (block, _) -> "minecraft:$block" }
		.mapValues { (_, item) -> "minecraft:$item" }

	/** The item name that places [block]; blocks with no mapping are their own item. */
	fun itemFor(block: String): String {
		EXPLICIT[block]?.let { return it }
		return when {
			block.contains("wall_") -> block.replace("wall_", "")
			block.contains("infested_") -> block.replace("infested_", "")
			block.contains("potted_") -> "minecraft:flower_pot"
			else -> block
		}
	}
}
