package dev.gaborwnuk.buildpaste.session

import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions as VanillaPermissions
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The three things a player can be allowed to do. */
enum class BuildPastePermission(val argumentName: String) {
	PASTE("pastePermission"),
	UPLOAD("uploadPermission"),
	CONSTRUCT("constructPermission");

	companion object {
		/** The argument that grants all three at once. */
		const val ALL = "allPermissions"

		fun byArgument(name: String): BuildPastePermission? = entries.find { it.argumentName == name }
	}
}

/**
 * Who may paste, upload and construct.
 *
 * Operators always may. Beyond that an operator can grant individual players a
 * permission at runtime, or open everything up with `/buildpaste allowall`.
 *
 * Grants are held in memory for the lifetime of the server, as they were in the plugin —
 * but keyed by UUID rather than by player object, so a grant is not silently lost when
 * the player reconnects.
 */
object Permissions {

	private val granted = ConcurrentHashMap<BuildPastePermission, MutableSet<UUID>>()

	/** Set by `/buildpaste allowall`, letting everyone use the mod. */
	@Volatile
	var everyoneAllowed: Boolean = false

	/**
	 * Whether the game considers this player an operator.
	 *
	 * Minecraft 26.1 replaced numeric permission levels with a permission set; the
	 * gamemaster permission is the one the old level 2 corresponded to.
	 */
	fun isOperator(player: ServerPlayer): Boolean =
		player.permissions().hasPermission(VanillaPermissions.COMMANDS_GAMEMASTER)

	fun has(player: ServerPlayer, permission: BuildPastePermission): Boolean =
		everyoneAllowed || isOperator(player) || granted[permission]?.contains(player.uuid) == true

	fun grant(uuid: UUID, permission: BuildPastePermission) {
		granted.computeIfAbsent(permission) { ConcurrentHashMap.newKeySet() }.add(uuid)
	}

	fun revoke(uuid: UUID, permission: BuildPastePermission) {
		granted[permission]?.remove(uuid)
	}

	fun grantAll(uuid: UUID) = BuildPastePermission.entries.forEach { grant(uuid, it) }

	fun revokeAll(uuid: UUID) = BuildPastePermission.entries.forEach { revoke(uuid, it) }
}
