package dev.gaborwnuk.buildpaste.event

import com.mojang.brigadier.CommandDispatcher
import dev.gaborwnuk.buildpaste.BuildPasteService
import dev.gaborwnuk.buildpaste.command.BuildPasteCommands
import dev.gaborwnuk.buildpaste.item.BuildPasteItems
import dev.gaborwnuk.buildpaste.paste.PasteScheduler
import dev.gaborwnuk.buildpaste.session.Sessions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * What the mod does in response to the game, with nothing loader-specific about it.
 *
 * Fabric and NeoForge deliver the same handful of moments through quite different event
 * systems, so each loader has a small entry point that listens its own way and calls in
 * here. Everything below is plain Minecraft API and is shared by both.
 *
 * All of it is server-side. The mod never registers anything on the client, which is what
 * lets a server run it without its players having it.
 */
object BuildPasteHooks {

	fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
		BuildPasteCommands.register(dispatcher)
	}

	/**
	 * Left-clicking a block with the position selector sets the first corner.
	 *
	 * Returns true when the click was the selector's, so the caller can stop the game from
	 * also starting to break the block.
	 */
	fun handleAttackBlock(player: ServerPlayer, stack: ItemStack, pos: BlockPos): Boolean {
		if (!BuildPasteItems.isPositionSelector(stack)) return false
		BuildPasteService.setPosition(player, pos.immutable(), first = true)
		return true
	}

	/** Right-clicking a block sets the second corner, or pastes if holding paste paper. */
	fun handleUseBlock(player: ServerPlayer, stack: ItemStack, pos: BlockPos): Boolean {
		if (BuildPasteItems.isPositionSelector(stack)) {
			BuildPasteService.setPosition(player, pos.immutable(), first = false)
			return true
		}
		return handleUseItem(player, stack)
	}

	/** Right-clicking with paste paper pastes the build written on it. */
	fun handleUseItem(player: ServerPlayer, stack: ItemStack): Boolean {
		val buildId = BuildPasteItems.pastePaperBuildId(stack) ?: return false
		BuildPasteService.paste(player, buildId, modifier = null)
		return true
	}

	fun onPlayerJoin(player: ServerPlayer) {
		Sessions[player]
	}

	fun onPlayerLeave(player: ServerPlayer) {
		Sessions.remove(player.uuid)
	}

	fun onServerTick() {
		PasteScheduler.tick()
	}

	fun onServerStopping() {
		PasteScheduler.clear()
		Sessions.clear()
	}
}
