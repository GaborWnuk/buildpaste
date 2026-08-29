package dev.gaborwnuk.buildpaste

import dev.gaborwnuk.buildpaste.event.EventHandlers
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge

/**
 * Mod entry point.
 *
 * Everything the mod does happens in response to a game event, so construction only has
 * to attach the handlers to the game event bus. Nothing is registered on the mod bus:
 * there are no blocks, items or entities of its own.
 */
@Mod(BuildPaste.MOD_ID)
class BuildPasteNeoForge {
	init {
		EventHandlers.register(NeoForge.EVENT_BUS)
		BuildPasteLog.info("BuildPaste ready")
	}
}
