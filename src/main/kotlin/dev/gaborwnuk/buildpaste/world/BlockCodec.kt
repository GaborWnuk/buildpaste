package dev.gaborwnuk.buildpaste.world

import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TagParser
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/**
 * Translates between block states in the world and the text form buildpaste.net stores.
 *
 * The backend keeps a block as a name plus a serialized property string such as
 * `[facing=north,half=top]`, which is the same shape the Bukkit plugin produced from
 * `BlockData.getAsString()`. Builds uploaded by that plugin are still on the backend, so
 * the text form has to stay byte-compatible in both directions.
 */
object BlockCodec {

	/**
	 * Update flags for placing a build's blocks.
	 *
	 * Clients are told about the change, but neighbour shape updates and block drops are
	 * suppressed: a paste sets every block itself, and letting the world react to each one
	 * in turn would both break the build and cost far more than the placement.
	 */
	const val PASTE_FLAGS: Int = Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_SUPPRESS_DROPS

	/**
	 * Properties dropped when reading a block.
	 *
	 * These carry state that should not travel with a build: the fill level of a cauldron,
	 * whether snow has settled on a block, and water that has seeped into a waterloggable
	 * one.
	 */
	private val PROPERTY_BLACKLIST = setOf("level=0", "snowy=false", "waterlogged=true")

	/** A block as the backend stores it: a namespaced name and its properties, if any. */
	data class Serialized(val name: String, val properties: String?)

	/**
	 * Reads a block state into the backend's text form.
	 *
	 * The original blanked blacklisted properties in place, leaving empty entries such as
	 * `[,facing=north]`; those strings reached the backend and are still stored there.
	 * Reading drops them cleanly, and [rotate] tolerates the older malformed form.
	 */
	fun serialize(state: BlockState): Serialized {
		val full = BlockStateParser.serialize(state)
		val bracket = full.indexOf('[')
		if (bracket < 0) return Serialized(full, null)

		val name = full.substring(0, bracket)
		val kept = full.substring(bracket + 1, full.length - 1)
			.split(',')
			.filter { it.isNotEmpty() && it.lowercase() !in PROPERTY_BLACKLIST }

		return Serialized(name, if (kept.isEmpty()) null else kept.joinToString(",", "[", "]"))
	}

	/**
	 * Parses a block descriptor, optionally carrying block-entity data, into something
	 * placeable.
	 *
	 * The descriptor is the same text `/setblock` accepts, so a chest with contents reads
	 * as `minecraft:chest[facing=north]{Items:[...]}`. Returns null for a block this game
	 * version does not know, which is expected: the shared block table predates 26.1 and a
	 * build may name blocks that were since renamed or removed.
	 */
	fun parse(lookup: HolderLookup<Block>, descriptor: String): BlockInput? =
		runCatching {
			val result = BlockStateParser.parseForBlock(lookup, descriptor, false)
			BlockInput(result.blockState(), result.properties().keys, result.nbt())
		}.getOrNull()

	/** The block lookup a level parses block names against. */
	fun lookupFor(level: ServerLevel): HolderLookup<Block> = level.registryAccess().lookupOrThrow(Registries.BLOCK)

	/**
	 * The block-entity contents at a position, in the SNBT form the descriptor grammar
	 * accepts, or null where the block holds none.
	 *
	 * This is what makes chests paste with their items and signs with their text. The
	 * Bukkit plugin declared an `nbt` field in the protocol but always sent it empty and
	 * ignored it on the way back, so every build it uploaded pastes hollow.
	 */
	fun captureNbt(level: ServerLevel, pos: BlockPos): String? {
		val blockEntity = level.getBlockEntity(pos) ?: return null
		return runCatching {
			val tag = blockEntity.saveWithoutMetadata(level.registryAccess())
			if (tag.isEmpty) null else tag.toString()
		}.getOrNull()
	}

	/**
	 * Builds the descriptor for a block, joining its name, rotated properties and any
	 * block-entity data into one string for [parse].
	 */
	fun describe(name: String, properties: String?, nbt: String?): String = buildString {
		append(name)
		if (properties != null && properties.startsWith("[")) append(properties)
		if (!nbt.isNullOrEmpty()) append(nbt)
	}

	/** Keys a block entity's own save never contains; they mark a full-metadata save. */
	private val METADATA_KEYS = listOf("x", "y", "z", "id")

	/** Fields that only ever appeared in pre-1.21 saves. */
	private val LEGACY_MARKERS = listOf("ForgeCaps", "NeoForgeCaps", "Text1")

	/**
	 * Prepares stored block-entity data for this version of the game, or returns null if it
	 * cannot be used.
	 *
	 * Builds on the backend go back to Minecraft 1.16, and a block entity saved then is laid
	 * out quite differently: signs kept `Text1` through `Text4` rather than front and back
	 * text, containers spelled their item slots differently, and Forge appended a
	 * `ForgeCaps` section. Restoring those faithfully would mean running the game's data
	 * fixers, and a build carries no data version to tell them which fixes to apply.
	 *
	 * Rather than hand the game something it can only half-read, tags in the old layout are
	 * dropped and the block is placed empty. That is what the plugin did with every block
	 * entity, so nothing is lost against it, and data written by this mod is in the current
	 * layout and comes back in full.
	 */
	fun sanitizeNbt(snbt: String?): String? {
		if (snbt.isNullOrBlank()) return null

		val tag = runCatching { TagParser.parseCompoundFully(snbt) }.getOrNull() ?: return null
		if (LEGACY_MARKERS.any(tag::contains)) return null

		METADATA_KEYS.forEach(tag::remove)
		return if (tag.isEmpty) null else tag.toString()
	}
}
