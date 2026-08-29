package dev.gaborwnuk.buildpaste

//? if neoforge {
import dev.gaborwnuk.buildpaste.event.BuildPasteHooks
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * NeoForge entry point.
 *
 * Nothing goes on the mod event bus: the mod registers no blocks, items or network
 * channels, and only listens for things happening in a running game.
 */
@Mod(BuildPaste.MOD_ID)
class BuildPasteNeoForge {
	init {
		val bus = NeoForge.EVENT_BUS

		bus.addListener<RegisterCommandsEvent> { event ->
			BuildPasteHooks.registerCommands(event.dispatcher)
		}

		bus.addListener<PlayerInteractEvent.LeftClickBlock> { event ->
			val player = actingPlayer(event) ?: return@addListener
			if (BuildPasteHooks.handleAttackBlock(player, event.itemStack, event.pos)) {
				// Cancel so that picking a corner does not also start breaking the block.
				event.isCanceled = true
			}
		}

		bus.addListener<PlayerInteractEvent.RightClickBlock> { event ->
			val player = actingPlayer(event) ?: return@addListener
			if (BuildPasteHooks.handleUseBlock(player, event.itemStack, event.pos)) {
				event.isCanceled = true
			}
		}

		bus.addListener<PlayerInteractEvent.RightClickItem> { event ->
			val player = actingPlayer(event) ?: return@addListener
			if (BuildPasteHooks.handleUseItem(player, event.itemStack)) {
				event.isCanceled = true
			}
		}

		bus.addListener<PlayerEvent.PlayerLoggedInEvent> { event ->
			(event.entity as? ServerPlayer)?.let(BuildPasteHooks::onPlayerJoin)
		}

		bus.addListener<PlayerEvent.PlayerLoggedOutEvent> { event ->
			(event.entity as? ServerPlayer)?.let(BuildPasteHooks::onPlayerLeave)
		}

		bus.addListener<ServerTickEvent.Post> { BuildPasteHooks.onServerTick() }
		bus.addListener<ServerStoppingEvent> { BuildPasteHooks.onServerStopping() }

		BuildPasteLog.info("BuildPaste ready")
	}

	/**
	 * The acting player, but only on the logical server.
	 *
	 * Interaction events fire on both sides, and everything the mod does edits the world,
	 * so the client-side pass must be ignored.
	 */
	private fun actingPlayer(event: PlayerInteractEvent): ServerPlayer? = when {
		event.level.isClientSide -> null
		event.hand != InteractionHand.MAIN_HAND -> null
		else -> event.entity as? ServerPlayer
	}
}
//?}
