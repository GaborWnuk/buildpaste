package dev.gaborwnuk.buildpaste.item

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * The two tools the mod hands out.
 *
 * Neither is a registered item: both are ordinary vanilla items carrying a custom name,
 * exactly as the Bukkit plugin made them. That keeps the mod free of registry entries,
 * so a world can be opened without it and the items simply become a plain stick and a
 * plain piece of paper.
 */
object BuildPasteItems {

	private const val SELECTOR_NAME = "Position Selector"

	/** A stick that sets the two corners of a selection by left- and right-clicking blocks. */
	fun positionSelector(): ItemStack = ItemStack(Items.STICK).apply {
		set(
			DataComponents.CUSTOM_NAME,
			Component.literal(SELECTOR_NAME).withStyle(ChatFormatting.AQUA),
		)
		set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
	}

	fun isPositionSelector(stack: ItemStack): Boolean =
		stack.`is`(Items.STICK) && customName(stack) == SELECTOR_NAME

	/**
	 * A piece of paper that pastes a particular build when right-clicked.
	 *
	 * The build id lives in the item's name, in brackets, which is how the plugin stored
	 * it and therefore how paper made by the plugin still reads.
	 */
	fun pastePaper(buildName: String?, buildId: String): ItemStack = ItemStack(Items.PAPER).apply {
		val label = if (buildName.isNullOrBlank()) "($buildId)" else "$buildName ($buildId)"
		set(DataComponents.CUSTOM_NAME, Component.literal(label).withStyle(ChatFormatting.LIGHT_PURPLE))
	}

	/** The build id written on a piece of paste paper, or null if this is ordinary paper. */
	fun pastePaperBuildId(stack: ItemStack): String? {
		if (!stack.`is`(Items.PAPER)) return null
		val name = customName(stack) ?: return null
		val open = name.lastIndexOf('(')
		val close = name.lastIndexOf(')')
		if (open < 0 || close < open + 1) return null
		return name.substring(open + 1, close).takeIf { it.isNotBlank() }
	}

	fun isPastePaper(stack: ItemStack): Boolean = pastePaperBuildId(stack) != null

	private fun customName(stack: ItemStack): String? = stack.get(DataComponents.CUSTOM_NAME)?.string
}
