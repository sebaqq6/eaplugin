package pl.eadventure.plugin.Events;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.Modules.ServerLogManager;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

public class playerPrivateChatEvent {
	//==================================================================================================================
	public static void onPlayerSendPrivateMessage(CommandSender source, Player target, String message) {
		if (source instanceof Player player) {
			if (!player.hasPermission("eadventureplugin.spyoverride")) {
				//log
				String pmLog = String.format("[SPY] %s -> %s: %s", source, target, message);
				ServerLogManager.log(pmLog, ServerLogManager.LogType.Chat);
				//spy
				Bukkit.getOnlinePlayers().forEach(p -> {
					PlayerData pd = PlayerData.get(p);
					if (pd.enabledSpy) {
						String msg = String.format("<#666699>%s <#66ff33>-> <#666699>%s:<#ffd966> %s", source.getName(), target.getName(), message);
						p.sendMessage(Utils.mm(msg));
					}
				});
			}
		}
	}

	//==================================================================================================================
	//----------------------------------------------ON PLAYER CHAT PROXY
	/*public void onPlayerChatProxy(AsyncPlayerChatEvent event) {
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
*/
	//-----------------------------------------------------------COMMAND PROXY
	/*public void onPlayerCommandProxy(PlayerCommandPreprocessEvent e) {
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
	 */
}
