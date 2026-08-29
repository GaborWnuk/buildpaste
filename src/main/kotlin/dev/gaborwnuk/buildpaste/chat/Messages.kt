package dev.gaborwnuk.buildpaste.chat

import dev.gaborwnuk.buildpaste.backend.BuildPasteApi
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import java.net.URI

/**
 * The clickable chat the mod sends.
 *
 * The Bukkit plugin built these with the BungeeCord chat API. The game's own component
 * API covers the same ground, with click and hover actions now being records rather than
 * an action enum plus a payload.
 */
object Messages {

	fun plain(text: String): MutableComponent = Component.literal(text)

	fun error(text: String): MutableComponent = Component.literal(text).withStyle(ChatFormatting.RED)

	/** A `[label]` that runs a command when clicked. */
	fun runButton(label: String, command: String, tooltip: String, color: ChatFormatting): MutableComponent =
		Component.literal(label).withStyle { style ->
			style.withColor(color)
				.withClickEvent(ClickEvent.RunCommand(command))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(tooltip)))
		}

	/** A `[label]` that types a command into the chat box without sending it. */
	fun suggestButton(label: String, command: String, tooltip: String, color: ChatFormatting): MutableComponent =
		Component.literal(label).withStyle { style ->
			style.withColor(color)
				.withClickEvent(ClickEvent.SuggestCommand(command))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(tooltip)))
		}

	/** A `[label]` that opens a web page. */
	fun linkButton(label: String, url: String, tooltip: String, color: ChatFormatting): MutableComponent =
		Component.literal(label).withStyle { style ->
			style.withColor(color)
				.withClickEvent(ClickEvent.OpenUrl(URI.create(url)))
				.withHoverEvent(HoverEvent.ShowText(Component.literal(tooltip)))
		}

	/** Wraps buttons in the `[ ] [ ]` row the plugin used. */
	fun buttonRow(vararg buttons: Component): MutableComponent {
		val row = Component.empty()
		buttons.forEachIndexed { index, button ->
			if (index > 0) row.append(" ")
			row.append("[").append(button).append("]")
		}
		return row
	}

	fun notConnected(player: ServerPlayer) {
		player.sendSystemMessage(
			plain("Accounts not connected, or another issue. To paste your selected build you need a ")
				.append(linkButton("[Buildpaste]", BuildPasteApi.SITE_ROOT, "Open buildpaste.net", ChatFormatting.GREEN))
				.append(" account. You can also paste a build by id, like this: ")
				.append(
					suggestButton(
						"[Paste]",
						"/paste p8LQhxa0lO7CxjafflKL",
						"Paste a build",
						ChatFormatting.LIGHT_PURPLE,
					)
				)
		)
	}

	fun selectionReady(player: ServerPlayer) {
		player.sendSystemMessage(
			plain("Look at the front of your build and type ").withStyle(ChatFormatting.GREEN)
				.append(
					runButton(
						"/upload",
						"/upload",
						"You can give it a name with /upload <name>",
						ChatFormatting.GREEN,
					)
				)
		)
	}

	fun uploadSucceeded(player: ServerPlayer, buildId: String) {
		player.sendSystemMessage(
			plain("Your upload was successful!\n").append(
				buttonRow(
					linkButton(
						"Publish",
						"${BuildPasteApi.SITE_ROOT}/upload?build=$buildId",
						"Make your build available for everyone",
						ChatFormatting.GREEN,
					),
					runButton("Copy", "/setbuild $buildId", "Select this build", ChatFormatting.AQUA),
					runButton("Share", "/sharebuild $buildId", "Share it with the server", ChatFormatting.LIGHT_PURPLE),
				)
			)
		)
	}

	fun shared(sharerName: String, buildId: String): Component =
		plain("$sharerName shared a build!\n").append(
			buttonRow(
				runButton("Paste", "/paste $buildId", "Paste the build", ChatFormatting.GREEN),
				runButton("Copy", "/setbuild $buildId", "Select this build", ChatFormatting.AQUA),
			)
		)

	fun about(): Component =
		plain("BuildPaste makes it easy to paste any kind of build into your world.\n").append(
			buttonRow(
				linkButton(
					"Visit buildpaste.net",
					BuildPasteApi.SITE_ROOT,
					"Open buildpaste.net to start",
					ChatFormatting.AQUA,
				),
				runButton(
					"Paste an example",
					"/paste qRi3mpfl4inn4juICRJO",
					"Paste an example build",
					ChatFormatting.GREEN,
				),
			)
		)
}
