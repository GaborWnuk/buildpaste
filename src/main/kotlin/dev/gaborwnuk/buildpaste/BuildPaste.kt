package dev.gaborwnuk.buildpaste

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Mod-wide constants. */
object BuildPaste {
	const val MOD_ID = "buildpaste"

	/** Permission level the game treats as an operator, matching the vanilla gamemaster tier. */
	const val OPERATOR_LEVEL = 2

	/** Blocks placed per server tick, carried over from the Bukkit plugin's paste budget. */
	const val BLOCKS_PER_TICK = 20_000

	/**
	 * Largest build this will attempt to place.
	 *
	 * The Bukkit plugin declared this limit but never enforced it, so an oversized build
	 * would run the server out of memory before it finished.
	 */
	const val MAX_PASTE_BLOCKS = 5_000_000L
}

/**
 * Mod logging.
 *
 * The plugin this was ported from broadcast its debug output to every player on the
 * server as chat messages. It goes to the log here, where a server owner can find it and
 * players are not made to read it.
 */
object BuildPasteLog {
	private val logger: Logger = LoggerFactory.getLogger(BuildPaste.MOD_ID)

	/** Set from `/buildpaste debug` to trace a paste that is misbehaving. */
	@Volatile
	var verbose: Boolean = false

	fun info(message: String) = logger.info(message)

	fun warn(message: String) = logger.warn(message)

	fun warn(message: String, error: Throwable) = logger.warn(message, error)

	fun debug(message: String, vararg arguments: Any?) {
		if (verbose) logger.info(message, *arguments) else logger.debug(message, *arguments)
	}
}
