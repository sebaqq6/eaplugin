package pl.eadventure.plugin.Events;

import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.ServerLogManager;

import java.util.Arrays;

public class playerPrivateChatEvent {
	//==================================================================================================================
	public static void onPlayerSendPrivateMessage(String source, String target, String message) {
		String pmLog = String.format("[SPY] %s -> %s: %s", source, target, message);
		ServerLogManager.log(pmLog, ServerLogManager.LogType.Chat);
	}

	//==================================================================================================================
	//----------------------------------------------ON PLAYER CHAT PROXY
	public void onPlayerChatProxy(AsyncPlayerChatEvent event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer(event.getPlayer());
				String chat = event.getMessage();

				if (mcp.hasConversation() && !mcp.isQuickChat()) {
					MineverseChatPlayer tp = MineverseChatAPI.getMineverseChatPlayer(mcp.getConversation());
					if (!tp.isOnline()) {//PRIVATE MESSAGE if player is offline
						if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
							String playerName = mcp.getPlayer().getName();
							String targetName = tp.getPlayer().getName();
							onPlayerSendPrivateMessage(playerName, targetName, chat);
						}
					} else {//if target player is online
						if (tp.getIgnores().contains(mcp.getUUID())) {
							return;
						}

						if (!tp.getMessageToggle()) {
							return;
						}

						if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
							String playerName = mcp.getPlayer().getName();
							String targetName = tp.getPlayer().getName();
							onPlayerSendPrivateMessage(playerName, targetName, chat);
						}
					}
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	//-----------------------------------------------------------COMMAND PROXY
	public void onPlayerCommandProxy(PlayerCommandPreprocessEvent e) {
		new BukkitRunnable() {
			@Override
			public void run() {
				Player p = e.getPlayer();
				String command = e.getMessage();
				String[] argsAll = command.split(" ");
				String[] args = Arrays.stream(argsAll)
						.skip(1)
						.toArray(String[]::new);

				if (argsAll[0].equalsIgnoreCase("/msg")) {//---------------------------------------------------/msg

					MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer(p);
					if (args.length == 0) return;
					MineverseChatPlayer player = MineverseChatAPI.getOnlineMineverseChatPlayer(args[0]);
					if (player == null) return;
					if (!mcp.getPlayer().canSee(player.getPlayer())) return;
					if (player.getIgnores().contains(mcp.getUUID())) return;
					if (!player.getMessageToggle()) return;
					if (args.length >= 2) {
						String msg = "";
						if (args[1].length() > 0) {
							for (int r = 1; r < args.length; ++r) {
								msg = msg + " " + args[r];
							}

							msg = msg.trim();

							if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
								onPlayerSendPrivateMessage(mcp.getName(), player.getName(), msg);
							}
						}

						if (args.length == 1 && args[0].length() > 0) {
							if (!mcp.hasConversation() || mcp.hasConversation() && !mcp.getConversation().toString().equals(player.getUUID().toString())) {

								if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
									onPlayerSendPrivateMessage(mcp.getName(), player.getName(), msg);
								}
							} else {
								if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
									onPlayerSendPrivateMessage(mcp.getName(), player.getName(), msg);
								}
							}
						}
					}
				} else if (argsAll[0].equalsIgnoreCase("/r")) {//----------------------------------------------------/r
					MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer(p);
					if (args.length <= 0) return;
					if (mcp.hasReplyPlayer()) {
						MineverseChatPlayer player = MineverseChatAPI.getOnlineMineverseChatPlayer(mcp.getReplyPlayer());
						if (player == null) return;
						if (!mcp.getPlayer().canSee(player.getPlayer())) return;
						if (player.getIgnores().contains(mcp.getUUID())) return;
						if (!player.getMessageToggle()) return;
						String msg = "";
						if (args.length > 0) {
							for (int r = 0; r < args.length; ++r) {
								msg = msg + " " + args[r];
							}

							msg = msg.trim();
							if (!mcp.getPlayer().hasPermission("venturechat.spy.override")) {
								onPlayerSendPrivateMessage(mcp.getName(), player.getName(), msg);
							}
						}
					}
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}
}
