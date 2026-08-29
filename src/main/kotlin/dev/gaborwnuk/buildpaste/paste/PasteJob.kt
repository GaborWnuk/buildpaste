package dev.gaborwnuk.buildpaste.paste

import dev.gaborwnuk.buildpaste.BuildPaste
import dev.gaborwnuk.buildpaste.BuildPasteLog
import dev.gaborwnuk.buildpaste.protocol.PlacementOrder
import dev.gaborwnuk.buildpaste.protocol.Rotation
import dev.gaborwnuk.buildpaste.session.PlayerSession
import dev.gaborwnuk.buildpaste.session.UndoRecord
import dev.gaborwnuk.buildpaste.world.BlockCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/** How a paste treats the world it lands on. */
enum class PasteMode {
	/** Place every block of the build, air included, clearing whatever stood there. */
	REPLACE,

	/** Leave the existing world wherever the build has air, so a build drops into a scene. */
	KEEP_EXISTING_UNDER_AIR;

	companion object {
		/** The modifier name players type, carried over from the original protocol. */
		const val DONT_PLACE_AIR = "dontplaceair"

		fun fromArgument(argument: String?): PasteMode =
			if (argument == DONT_PLACE_AIR) KEEP_EXISTING_UNDER_AIR else REPLACE
	}
}

/**
 * One running paste.
 *
 * A build may run to millions of blocks, far more than fits in a tick, so a job is
 * stepped by [PasteScheduler] a bounded number of blocks at a time and reports itself
 * finished when the walk runs out.
 */
class PasteJob(
	private val level: ServerLevel,
	private val session: PlayerSession,
	private val origin: BlockPos,
	private val sizeX: Int,
	private val sizeY: Int,
	private val sizeZ: Int,
	private val blocks: List<String>,
	private val data: List<String?>,
	private val nbt: Map<Int, String>,
	/**
	 * The direction the build was uploaded facing. Equal to [rotateTo] when the build
	 * should be placed exactly as recorded, which is what an undo needs.
	 */
	private val rotateFrom: String,
	private val rotateTo: String,
	private val mode: PasteMode,
	private val constructing: Boolean,
) {
	private val order = PlacementOrder(
		origin.x, origin.y, origin.z, sizeX, sizeY, sizeZ, rotateFrom, rotateTo,
	)
	private val lookup = BlockCodec.lookupFor(level)
	private val cursor = BlockPos.MutableBlockPos()

	private var index = 0
	private var placed = 0
	private var skippedUnknown = 0
	private var skippedUnaffordable = 0
	private var skippedLegacyNbt = 0

	/** What stood here before, captured as the walk goes so the paste can be undone. */
	private val previousBlocks = ArrayList<String>(blocks.size)
	private val previousData = ArrayList<String?>(blocks.size)
	private val previousNbt = HashMap<Int, String>()

	/** Items spent while constructing, taken from the inventory when the job finishes. */
	private val spentItems = HashMap<String, Int>()

	private val total: Int =
		if (!order.isValid) 0
		else minOf(blocks.size.toLong(), data.size.toLong(), order.count).toInt()

	val isComplete: Boolean get() = index >= total

	val blockCount: Int get() = total

	/** Places up to [budget] blocks. Returns false if the job should be abandoned. */
	fun step(budget: Int): Boolean {
		val player = level.server.playerList.getPlayer(session.uuid)
		if (player == null) {
			BuildPasteLog.debug("Abandoning paste for {}: player left", session.playerName)
			session.pasteInProgress = false
			return false
		}

		var remaining = budget
		while (remaining > 0 && index < total) {
			placeOne(index)
			index++
			remaining--
		}

		if (isComplete) finish(player)
		return true
	}

	private fun placeOne(i: Int) {
		order.positionAt(i) { x, y, z ->
			cursor.set(x, y, z)

			// Record what is here first, for every position the walk visits — including the
			// ones this paste decides to leave alone — so an undo restores the whole region
			// exactly as it was found.
			capturePrevious(i)

			val name = blocks[i]
			val rotated = Rotation.rotateProperties(data[i], rotateFrom, rotateTo)

			val storedNbt = nbt[i]
			val blockNbt = BlockCodec.sanitizeNbt(storedNbt)
			if (storedNbt != null && blockNbt == null) skippedLegacyNbt++

			val descriptor = BlockCodec.describe(name, rotated, blockNbt)
			val input = BlockCodec.parse(lookup, descriptor)

			if (input == null) {
				// The shared block table predates 26.1, so a build may name a block this
				// version renamed or removed. Leaving the world alone beats punching a hole in it.
				if (skippedUnknown == 0) {
					BuildPasteLog.debug("Skipping block this version does not know: {}", descriptor)
				}
				skippedUnknown++
				return@positionAt
			}

			if (mode == PasteMode.KEEP_EXISTING_UNDER_AIR && input.state.isAir) return@positionAt

			if (constructing && !spendMaterialFor(name)) {
				skippedUnaffordable++
				return@positionAt
			}

			// Comparing the whole state, not just the block type, means pasting over an existing
			// copy of a build still corrects stairs and logs that face the wrong way. The
			// original compared only the type and left them turned the wrong way.
			if (blockNbt == null && level.getBlockState(cursor) == input.state) return@positionAt

			input.place(level, cursor, BlockCodec.PASTE_FLAGS)
			placed++
		}
	}

	private fun capturePrevious(blockIndex: Int) {
		val existing = level.getBlockState(cursor)
		val serialized = BlockCodec.serialize(existing)
		previousBlocks.add(serialized.name)
		previousData.add(serialized.properties)
		if (!existing.isAir) {
			BlockCodec.captureNbt(level, cursor)?.let { previousNbt[blockIndex] = it }
		}
	}

	/** Takes one of the item that places [block] from the tally, if the player has any left. */
	private fun spendMaterialFor(block: String): Boolean {
		if (block == AIR_BLOCK) return true
		val item = BlockItemMapping.itemFor(block)
		val available = session.availableItems[item] ?: 0
		if (available <= 0) return false
		session.availableItems[item] = available - 1
		spentItems.merge(item, 1, Int::plus)
		return true
	}

	private fun finish(player: ServerPlayer) {
		// An undo replays the captured blocks through the same walk, so it must carry the same
		// direction pair. Both are set to the paste direction so that rotation is a no-op:
		// what was captured is already oriented the way the world had it.
		session.undo = UndoRecord(
			levelKey = level.dimension(),
			origin = origin,
			sizeX = sizeX,
			sizeY = sizeY,
			sizeZ = sizeZ,
			uploadDirection = rotateTo,
			pasteDirection = rotateTo,
			blocks = previousBlocks,
			data = previousData,
			nbt = previousNbt,
		)
		session.pasteInProgress = false

		if (constructing) InventoryBilling.take(player, spentItems)

		BuildPasteLog.debug(
			"Paste finished for {}: {} placed, {} unknown, {} unaffordable, {} legacy block entities",
			session.playerName, placed, skippedUnknown, skippedUnaffordable, skippedLegacyNbt,
		)

		if (skippedUnknown > 0) {
			player.sendSystemMessage(
				Component.literal("$skippedUnknown block(s) skipped: this Minecraft version does not have them.")
					.withStyle(ChatFormatting.YELLOW)
			)
		}
		if (skippedLegacyNbt > 0) {
			player.sendSystemMessage(
				Component.literal(
					"$skippedLegacyNbt block(s) were placed empty: their contents were saved by a " +
						"much older version of Minecraft and cannot be read."
				).withStyle(ChatFormatting.YELLOW)
			)
		}
		if (skippedUnaffordable > 0) {
			player.sendSystemMessage(
				Component.literal("$skippedUnaffordable block(s) skipped: you ran out of materials.")
					.withStyle(ChatFormatting.YELLOW)
			)
		}
	}

	companion object {
		private const val AIR_BLOCK = "minecraft:air"

		const val DEFAULT_BUDGET = BuildPaste.BLOCKS_PER_TICK
	}
}
