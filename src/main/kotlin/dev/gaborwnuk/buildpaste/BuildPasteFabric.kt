package dev.gaborwnuk.buildpaste

//? if fabric {
/*import dev.gaborwnuk.buildpaste.event.BuildPasteHooks
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult

/**
 * Fabric entry point.
 *
 * The mod registers no content, so this only attaches listeners. Each one narrows the
 * event to a server-side main-hand interaction and hands off to the shared hooks.
 */
object BuildPasteFabric : ModInitializer {

	override fun onInitialize() {
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			BuildPasteHooks.registerCommands(dispatcher)
		}

		AttackBlockCallback.EVENT.register { player, _, hand, pos, _ ->
			val serverPlayer = actingPlayer(player, hand)
			if (serverPlayer != null && BuildPasteHooks.handleAttackBlock(serverPlayer, player.mainHandItem, pos)) {
				// Consume so that picking a corner does not also start breaking the block.
				InteractionResult.SUCCESS
			} else {
				InteractionResult.PASS
			}
		}

		UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
			val serverPlayer = actingPlayer(player, hand)
			if (serverPlayer != null &&
				BuildPasteHooks.handleUseBlock(serverPlayer, player.mainHandItem, hitResult.blockPos)
			) {
				InteractionResult.SUCCESS
			} else {
				InteractionResult.PASS
			}
		}

		UseItemCallback.EVENT.register { player, _, hand ->
			val serverPlayer = actingPlayer(player, hand)
			if (serverPlayer != null && BuildPasteHooks.handleUseItem(serverPlayer, player.mainHandItem)) {
				InteractionResult.SUCCESS
			} else {
				InteractionResult.PASS
			}
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			BuildPasteHooks.onPlayerJoin(handler.player)
		}

		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
			BuildPasteHooks.onPlayerLeave(handler.player)
		}

		ServerTickEvents.END_SERVER_TICK.register { BuildPasteHooks.onServerTick() }
		ServerLifecycleEvents.SERVER_STOPPING.register { BuildPasteHooks.onServerStopping() }

		BuildPasteLog.info("BuildPaste ready")
	}

	/**
	 * The acting player, but only for a server-side main-hand interaction.
	 *
	 * Interaction callbacks fire on both sides, and everything the mod does edits the
	 * world, so the client-side pass must be ignored.
	 */
	private fun actingPlayer(player: net.minecraft.world.entity.player.Player, hand: InteractionHand): ServerPlayer? =
		if (hand != InteractionHand.MAIN_HAND) null else player as? ServerPlayer
}
*///?}
