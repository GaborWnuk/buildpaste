package dev.gaborwnuk.buildpaste.session

import dev.gaborwnuk.buildpaste.protocol.BuildData
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * What a build paste can be undone back to.
 *
 * [blocks] and [data] are indexed the same way the pasted build was, so replaying them
 * through the same placement walk puts every previous block back where it came from.
 */
class UndoRecord(
	val levelKey: ResourceKey<Level>,
	val origin: BlockPos,
	val sizeX: Int,
	val sizeY: Int,
	val sizeZ: Int,
	val uploadDirection: String,
	val pasteDirection: String,
	val blocks: List<String>,
	val data: List<String?>,
	val nbt: Map<Int, String>,
)

/**
 * Per-player state for the duration of a session.
 *
 * The Bukkit plugin held this as twenty-odd static HashMaps keyed by UUID string, seeded
 * on join and never cleaned up on quit. Gathering it into one object per player means it
 * can be dropped when they disconnect.
 */
class PlayerSession(val uuid: UUID, var playerName: String) {

	/** The two corners of the region `/upload` will read, and the world they were set in. */
	var pos1: BlockPos? = null
	var pos2: BlockPos? = null
	var selectionLevel: ResourceKey<Level>? = null

	/**
	 * The most recently downloaded build, kept so that pasting the same id twice does not
	 * fetch it again.
	 */
	var cachedBuildId: String? = null
	var cachedBuild: BuildData? = null

	/** The id the last `/upload` produced, for the follow-up links sent to the player. */
	var uploadedBuildId: String? = null

	/** What the last paste overwrote. */
	var undo: UndoRecord? = null

	/** Item counts from the player's inventory, gathered when `/construct` is planned. */
	val availableItems: MutableMap<String, Int> = mutableMapOf()

	/** Block counts the planned build needs. */
	val requiredBlocks: MutableMap<String, Int> = mutableMapOf()

	/** True while a paste is running, so a second one cannot interleave with it. */
	var pasteInProgress: Boolean = false

	fun clearSelection() {
		pos1 = null
		pos2 = null
		selectionLevel = null
	}

	fun cache(buildId: String, build: BuildData) {
		cachedBuildId = buildId
		cachedBuild = build
	}

	fun cached(buildId: String): BuildData? = if (cachedBuildId == buildId) cachedBuild else null
}

/** The live sessions, one per connected player. */
object Sessions {
	private val sessions = ConcurrentHashMap<UUID, PlayerSession>()

	operator fun get(player: ServerPlayer): PlayerSession =
		sessions.compute(player.uuid) { uuid, existing ->
			existing?.also { it.playerName = player.gameProfile.name }
				?: PlayerSession(uuid, player.gameProfile.name)
		}!!

	fun remove(uuid: UUID) {
		sessions.remove(uuid)
	}

	fun clear() = sessions.clear()
}
