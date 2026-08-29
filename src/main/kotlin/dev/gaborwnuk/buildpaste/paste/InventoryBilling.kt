package dev.gaborwnuk.buildpaste.paste

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * Counts and charges the materials a `/construct` uses.
 *
 * The Bukkit plugin took payment by dispatching a `clear <player> <item> <count>` console
 * command once the paste finished. Editing the inventory directly does the same job
 * without going through the command system, and without depending on the console holding
 * permission to run commands on a player.
 */
object InventoryBilling {

	/** Every item the player is carrying, by namespaced name. */
	fun gather(player: ServerPlayer): Map<String, Int> {
		val counts = mutableMapOf<String, Int>()
		val inventory = player.inventory
		for (slot in 0 until inventory.containerSize) {
			val stack = inventory.getItem(slot)
			if (stack.isEmpty) continue
			counts.merge(nameOf(stack), stack.count, Int::plus)
		}
		return counts
	}

	/** Removes [items] from the player's inventory, stopping early if a stack runs out. */
	fun take(player: ServerPlayer, items: Map<String, Int>) {
		if (items.isEmpty()) return
		val outstanding = items.toMutableMap()
		val inventory = player.inventory

		for (slot in 0 until inventory.containerSize) {
			if (outstanding.isEmpty()) break
			val stack = inventory.getItem(slot)
			if (stack.isEmpty) continue

			val name = nameOf(stack)
			val owed = outstanding[name] ?: continue
			val taken = minOf(owed, stack.count)

			inventory.removeItem(slot, taken)
			if (owed - taken <= 0) outstanding.remove(name) else outstanding[name] = owed - taken
		}
	}

	private fun nameOf(stack: ItemStack): String = BuiltInRegistries.ITEM.getKey(stack.item).toString()
}
