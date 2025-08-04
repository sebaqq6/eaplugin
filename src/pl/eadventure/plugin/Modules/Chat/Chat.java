package pl.eadventure.plugin.Modules.Chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat implements Listener { // Implement the ChatRenderer and Listener interface
	public static Channel globalChannel;

	public static void init() {
		//create channels
		Channel channelTemp = null;
		//global
		channelTemp = new Channel("Globalny", "/czatglobalny", null, true);
		channelTemp.setChannelPrefix("");
		channelTemp.setFormat("%eaplugin_chatchannel_prefix% %vault_prefix%%player_displayname%<#FFFFFF>: ");
		globalChannel = channelTemp;
		//local
		channelTemp = new Channel("Lokalny", "/czatlokalny", null, true);
		channelTemp.setDistance(100);
		channelTemp.setChannelPrefix("<#D4FF00>[Lokalny]</#D4FF00>");
		channelTemp.setFormat("<yellow>%eaplugin_chatchannel_prefix% <#FFAA00>%player_displayname%</#FFAA00><#FFFFFF>:</#FFFFFF> ");
		//admin
		channelTemp = new Channel("Admin", "/adminczat", "eadventureplugin.cmd.adminczat", false);
		channelTemp.setChannelPrefix("<#AA0000>[Adminchat]</#AA0000>");
		channelTemp.setFormat("<red>%eaplugin_chatchannel_prefix% <#D70000>%player_displayname%</#D70000><#FFFFFF>:</#FFFFFF> ");
	}

	@EventHandler
	public void onChat(AsyncChatEvent e) {
		Component originalMessage = e.message();
		Component updatedMessage = originalMessage;
		Player sourcePlayer = e.getPlayer();
		String messageString = PlainTextComponentSerializer.plainText().serialize(originalMessage);
		String sourceDisplayNameString = PlainTextComponentSerializer.plainText().serialize(sourcePlayer.displayName());
		PlayerData sourcePlayerData = PlayerData.get(sourcePlayer);
		Channel sourceChannel = sourcePlayerData.chatChannel == null ? (globalChannel) : (sourcePlayerData.chatChannel);
		//channel other than global?
		if (sourceChannel != globalChannel) {
			//cancel send message when not permission
			if (sourceChannel.getChannelPermission() != null && !sourcePlayer.hasPermission(sourceChannel.getChannelPermission())) {
				e.setCancelled(true);
				return;
			}
			//filter players
			Iterator<Audience> iterator = e.viewers().iterator();
			while (iterator.hasNext()) {
				Audience viewer = iterator.next();
				if (viewer instanceof Player targetViewer) {
					if (!sourceChannel.getViewers(sourcePlayer).contains(targetViewer)) {
						iterator.remove();
					}
				}
			}
		}
		//placeholders
		updatedMessage = parseChatPlaceholders(sourcePlayer, originalMessage);
		//modify messsage
		e.message(updatedMessage);

		//=-=-=-=-=-=-=-=-=-=-=-=-=Renderer section=-=-=-=-=-=-=-=-=-=-=-=-=

		e.renderer((source, sourceDisplayName, message, viewer) -> {
			/*Channel targetChannel = null;
			PlayerData playerDataTarget = null;
			if (viewer instanceof Player target) {
				playerDataTarget = PlayerData.get(target);
				targetChannel = PlayerData.get(target).chatChannel == null ? (globalChannel) : (playerDataTarget.chatChannel);
			}*/
			String format = PlaceholderAPI.setPlaceholders(source, sourceChannel.getFormat());
			//parse message
			String beforeMessage = String.format("%s: %s", sourceDisplayNameString, messageString);
			// beforeMessage = String.format("%s<!bold>%s<white>: ", convertToMiniMessage(vaultPrefix), sourceDisplayNameString);
			beforeMessage = String.format("%s", format);

			//set final component
			Component finalMessage = Utils.mm(beforeMessage).append(message);
			//Component finalMessage = Utils.mm(beforeMessage).append(message.color(TextColor.fromHexString("#e6fffe")));
			//Console new message (not secure tag blocked)
			if (viewer instanceof ConsoleCommandSender) {
				Bukkit.getConsoleSender().sendMessage(finalMessage);
			}
			return finalMessage;
		});
	}

	//auto join to channels
	public static void autoJoinChannels(Player player) {
		PlayerData pd = PlayerData.get(player);
		for (Channel channel : Channel.channelList) {
			if (channel.autoJoin || (channel.getChannelPermission() != null && player.hasPermission(channel.getChannelPermission()))) {
				pd.joinedChatChannels.add(channel);
				print.debug("Gracz " + player.getName() + " dołącza do czatu: " + channel.getChannelName());
			}
		}
	}

	public static Component parseChatPlaceholders(Player player, Component message) {
		Component updatedMessage = message;
		String messageString = PlainTextComponentSerializer.plainText().serialize(message);
		//placeholder [item]
		if (messageString.contains("[item]")) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack != null && !itemStack.getType().isAir()) {
				Component itemName = itemStack.displayName();
				updatedMessage = message.replaceText(builder -> builder
						.matchLiteral("[item]")
						.replacement(itemName)
				);
			}
		}
		//next one
		return updatedMessage;
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
