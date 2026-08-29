package dev.gaborwnuk.buildpaste.event

import dev.gaborwnuk.buildpaste.BuildPasteService
import dev.gaborwnuk.buildpaste.command.BuildPasteCommands
import dev.gaborwnuk.buildpaste.item.BuildPasteItems
import dev.gaborwnuk.buildpaste.paste.PasteScheduler
import dev.gaborwnuk.buildpaste.session.Sessions
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * Wires the mod into the game's events.
 *
 * All of this is server-side: the mod registers nothing on the client and reacts only to
 * things that happen on the logical server, which is what lets it run on a server whose
 * players do not have it installed.
 */
object EventHandlers {

	fun register(bus: IEventBus) {
		bus.addListener(::onRegisterCommands)
		bus.addListener(::onLeftClickBlock)
		bus.addListener(::onRightClickBlock)
		bus.addListener(::onRightClickItem)
		bus.addListener(::onPlayerLoggedIn)
		bus.addListener(::onPlayerLoggedOut)
		bus.addListener(::onServerTick)
		bus.addListener(::onServerStopping)
	}

	private fun onRegisterCommands(event: RegisterCommandsEvent) {
		BuildPasteCommands.register(event.dispatcher)
	}

	/** Left-clicking a block with the position selector sets the first corner. */
	private fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
		val player = serverPlayer(event) ?: return
		if (event.hand != InteractionHand.MAIN_HAND) return
		if (!BuildPasteItems.isPositionSelector(event.itemStack)) return

		BuildPasteService.setPosition(player, event.pos.immutable(), first = true)
		// Cancel so that selecting a corner does not also start breaking the block.
		event.isCanceled = true
	}

	/** Right-clicking a block sets the second corner, or pastes if holding paste paper. */
	private fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
		val player = serverPlayer(event) ?: return
		if (event.hand != InteractionHand.MAIN_HAND) return

		when {
			BuildPasteItems.isPositionSelector(event.itemStack) -> {
				BuildPasteService.setPosition(player, event.pos.immutable(), first = false)
				event.isCanceled = true
			}
			else -> {
				val buildId = BuildPasteItems.pastePaperBuildId(event.itemStack) ?: return
				BuildPasteService.paste(player, buildId, modifier = null)
				event.isCanceled = true
			}
		}
	}

	/** Right-clicking the air with paste paper pastes its build. */
	private fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
		val player = serverPlayer(event) ?: return
		if (event.hand != InteractionHand.MAIN_HAND) return

		val buildId = BuildPasteItems.pastePaperBuildId(event.itemStack) ?: return
		BuildPasteService.paste(player, buildId, modifier = null)
		event.isCanceled = true
	}

	private fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
		(event.entity as? ServerPlayer)?.let { Sessions[it] }
	}

	private fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
		(event.entity as? ServerPlayer)?.let { Sessions.remove(it.uuid) }
	}

	private fun onServerTick(event: ServerTickEvent.Post) {
		PasteScheduler.tick()
	}

	private fun onServerStopping(event: ServerStoppingEvent) {
		PasteScheduler.clear()
		Sessions.clear()
	}

	/**
	 * The acting player, but only on the logical server.
	 *
	 * Interaction events fire on both sides; everything here edits the world, so the
	 * client-side pass must be ignored.
	 */
	private fun serverPlayer(event: PlayerInteractEvent): ServerPlayer? =
		if (event.level.isClientSide) null else event.entity as? ServerPlayer
}
