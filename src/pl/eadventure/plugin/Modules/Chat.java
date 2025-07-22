package pl.eadventure.plugin.Modules;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat implements Listener, ChatRenderer { // Implement the ChatRenderer and Listener interface

	// Listen for the AsyncChatEvent
	// Global processing
	@EventHandler
	public void onChat(AsyncChatEvent e) {
		Component originalMessage = e.message();
		Component updatedMessage = originalMessage;
		String messageString = PlainTextComponentSerializer.plainText().serialize(originalMessage);
		//placeholder [item]
		if (messageString.contains("[item]")) {
			ItemStack itemStack = e.getPlayer().getInventory().getItemInMainHand();
			if (itemStack != null && !itemStack.getType().isAir()) {
				Component itemName = itemStack.displayName();
				updatedMessage = originalMessage.replaceText(builder -> builder
						.matchLiteral("[item]")
						.replacement(itemName)
				);
			}
		}
		e.message(updatedMessage);
		e.renderer(this);
	}

	// Render per player
	@Override
	public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
		String sourceDisplayNameString = PlainTextComponentSerializer.plainText().serialize(sourceDisplayName);
		String messageString = PlainTextComponentSerializer.plainText().serialize(message);
		String vaultPrefix = PlaceholderAPI.setPlaceholders(source, "%vault_prefix%");

		String beforeMessage = String.format("%s: %s", sourceDisplayNameString, messageString);
		beforeMessage = String.format("%s<!bold>%s<white>: ", convertToMiniMessage(vaultPrefix), sourceDisplayNameString);

		//Final component
		Component finalMessage = Utils.mm(beforeMessage).append(message);
		//Console new message (not secure tag blocked)
		if (viewer instanceof ConsoleCommandSender) {
			Bukkit.getConsoleSender().sendMessage(finalMessage);
		}
		return finalMessage;
	}

	public static String convertToMiniMessage(String input) {
		// Najpierw HEXY: #RRGGBB → <#RRGGBB>
		Matcher hexMatcher = HEX_PATTERN.matcher(input);
		StringBuffer hexBuffer = new StringBuffer();
		while (hexMatcher.find()) {
			String hex = hexMatcher.group();
			hexMatcher.appendReplacement(hexBuffer, "<" + hex + ">");
		}
		hexMatcher.appendTail(hexBuffer);
		String hexProcessed = hexBuffer.toString();

		// Teraz LEGACY: &a, &l itd.
		Matcher legacyMatcher = LEGACY_PATTERN.matcher(hexProcessed);
		StringBuffer result = new StringBuffer();
		while (legacyMatcher.find()) {
			char code = Character.toLowerCase(legacyMatcher.group(1).charAt(0));
			String replacement;

			if (LEGACY_COLOR_MAP.containsKey(code)) {
				// Kolor → <reset><kolor>
				replacement = "<reset>" + LEGACY_COLOR_MAP.get(code);
			} else if (LEGACY_FORMATTING_MAP.containsKey(code)) {
				// Styl (bold, italic itd.)
				replacement = LEGACY_FORMATTING_MAP.get(code);
			} else {
				// Nieznany kod (nie powinno wystąpić)
				replacement = "";
			}

			legacyMatcher.appendReplacement(result, replacement);
		}
		legacyMatcher.appendTail(result);

		return result.toString();
	}

	private static final Map<Character, String> LEGACY_COLOR_MAP = Map.ofEntries(
			Map.entry('0', "<black>"),
			Map.entry('1', "<dark_blue>"),
			Map.entry('2', "<dark_green>"),
			Map.entry('3', "<dark_aqua>"),
			Map.entry('4', "<dark_red>"),
			Map.entry('5', "<dark_purple>"),
			Map.entry('6', "<gold>"),
			Map.entry('7', "<gray>"),
			Map.entry('8', "<dark_gray>"),
			Map.entry('9', "<blue>"),
			Map.entry('a', "<green>"),
			Map.entry('b', "<aqua>"),
			Map.entry('c', "<red>"),
			Map.entry('d', "<light_purple>"),
			Map.entry('e', "<yellow>"),
			Map.entry('f', "<white>")
	);

	private static final Map<Character, String> LEGACY_FORMATTING_MAP = Map.ofEntries(
			Map.entry('l', "<bold>"),
			Map.entry('o', "<italic>"),
			Map.entry('n', "<underlined>"),
			Map.entry('m', "<strikethrough>"),
			Map.entry('k', "<obfuscated>"),
			Map.entry('r', "<reset>")
	);

	private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
	private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

}
