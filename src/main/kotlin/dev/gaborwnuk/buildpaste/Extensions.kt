package dev.gaborwnuk.buildpaste

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/**
 * The server a player is on.
 *
 * `ServerPlayer` keeps its own server reference private, so it is reached through the
 * level. The server doubles as the executor for the main thread, which is how work that
 * started on an HTTP thread gets back somewhere it may touch the world.
 */
internal val ServerPlayer.gameServer: MinecraftServer
	get() = level().server
