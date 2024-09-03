package pl.eadventure.plugin.Commands;

import com.sk89q.worldedit.world.RegenOptions;
import ct.ajneb97.utils.UtilsPlayers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.*;
import pl.eadventure.plugin.Events.leavesDecayEvent;
import pl.eadventure.plugin.Utils.*;

import java.io.IOException;
import java.util.*;

public class Command_eap implements TabExecutor {
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (args.length == 1) {
			List<String> cmdlist = Arrays.asList(
					"debug",
					"decay",
					"decaydebug",
					"decaystats",
					"reloadbans",
					"syncplayedtime",
					"addcustomitem",
					"getcustomitem",
					"armorfixreload",
					"plist",
					"rcl");
			return StringUtil.copyPartialMatches(args[0], cmdlist, new ArrayList<>());
		}
		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0)
			return usage(sender);
		switch (args[0].toLowerCase()) {
			// Komenda: /eap debug
			case "debug": {
				if (print.getDebug()) {
					print.debug("Wyłączanie debugowania...");
					print.setDebug(false);
					sender.sendMessage("Wyłączono debugowanie pluginu Eternal Adventure.");
				} else {
					print.setDebug(true);
					print.debug("Debugowanie włączone!");
					sender.sendMessage("Włączono debugowanie pluginu Eternal Adventure.");
				}
				break;
			}
			case "decay": {
				if (LeavesDecay.enabled()) {
					LeavesDecay.active(false);
					sender.sendMessage("Wyłączono automatyczne usuwanie liści przez Eternal Adventure Plugin.");
				} else {
					LeavesDecay.active(true);
					sender.sendMessage("Włączono automatyczne usuwanie liści przez Eternal Adventure Plugin.");
				}
				EternalAdventurePlugin.getMainConfig().set("leavesDecaySystem", LeavesDecay.enabled());
				EternalAdventurePlugin.getInstance().saveConfig();
				break;
			}
			case "decaydebug": {
				if (sender instanceof Player player) {
					PlayerData pd = PlayerData.get(player);
					if (pd.decayDebug) {
						pd.decayDebug = false;
						sender.sendMessage("Wyłączono debugowanie liści.");
					} else {
						pd.decayDebug = true;
						sender.sendMessage("Włączono debugowanie liści. Użyj PPM klikając na blok aby sprawdzić liść.");
					}
				}

				break;
			}
			case "decaystats": {
				leavesDecayEvent.getStats(sender);
				break;
			}
			case "decaysleepthread": {
				if (LeavesDecay.enabled()) {
					sender.sendMessage("Najpierw wyłącz system: /eap decay");
					return true;
				}
				if (args.length == 1) {
					sender.sendMessage("Użyj: /eap decaysleepthread [sleep ms]");
					return true;
				}
				Integer sleepThreadTime;
				try {
					sleepThreadTime = Integer.valueOf(args[1]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.color("Wartość nie jest cyfrą."));
					return true;
				}
				leavesDecayEvent.setThreadSleep(sleepThreadTime);
				sender.sendMessage("Ustawiono decay sleep thread: " + sleepThreadTime);
				break;
			}
			case "jump": {
				if (sender instanceof Player) {
					Player p = (Player) sender;
					World w = p.getWorld();
					Location actualPos = p.getLocation();
					double x = actualPos.getX();
					double y = actualPos.getY() + 50.0;
					double z = actualPos.getZ();
					Location newLocation = new Location(w, x, y, z);
					p.teleport(newLocation);
					sender.sendMessage("Hooop!");
				} else {
					print.error("Dostępne tylko z poziomu gracza!");
				}
				break;
			}
			case "test": {
				sender.sendMessage("Testowanie...");
				if (args.length == 1) {
					sender.sendMessage("Użyj: /eap test [testid]");
					return true;
				}
				String testid = args[1];

				if (testid.equalsIgnoreCase("on")) {
					Player player = (Player) sender;
					sender.sendMessage("Włączone");
					player.setMetadata("eapenabledturrets", new FixedMetadataValue(EternalAdventurePlugin.getInstance(), true));
				} else if (testid.equalsIgnoreCase("off")) {
					Player player = (Player) sender;
					sender.sendMessage("Wyłączone");
					Iterator<MetadataValue> var2 = player.getMetadata("eapenabledturrets").iterator();

					while (var2.hasNext()) {
						MetadataValue var1 = var2.next();
						Plugin ownPlugin = var1.getOwningPlugin();
						print.debug(ownPlugin.toString());
						player.removeMetadata("eapenabledturrets", ownPlugin);
					}

				} else if (testid.equalsIgnoreCase("status")) {
					Player player = (Player) sender;
					if (!player.hasMetadata("eapenabledturrets")) {
						sender.sendMessage("Brak metadanych eapenabledturrets");
						return true;
					}
					MetadataValue metadataValue = player.getMetadata("eapenabledturrets").get(0);
					if (metadataValue.asBoolean()) {
						sender.sendMessage("Metadata eapenabledturrets: TRUE");
					} else {
						sender.sendMessage("Metadata eapenabledturrets: FALSE");
					}
				} else if (testid.equalsIgnoreCase("hack")) {
					try {
						CrackComplexTurret.crack();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else if (testid.equalsIgnoreCase("survival")) {
					Player player = (Player) sender;
					print.debug("IsSurvival; " + UtilsPlayers.isSurvival(player));
				} else if (testid.equalsIgnoreCase("list")) {
					Player player = (Player) sender;
					Iterator var2 = player.getMetadata("eapenabledturrets").iterator();

					while (var2.hasNext()) {
						MetadataValue var1 = (MetadataValue) var2.next();
						sender.sendMessage(var1.asString());
					}
				} else if (testid.equalsIgnoreCase("disablebs")) {
					CrackComplexTurret.disabledBypass = true;
				} else if (testid.equalsIgnoreCase("enablebs")) {
					CrackComplexTurret.disabledBypass = false;
				}
				break;
			}
			case "givemoney": {
				if (sender instanceof Player) {
					sender.sendMessage("Komenda dostępna tylko z konsoli.");
					return true;
				}
				if (args.length == 1) {
					sender.sendMessage("Użyj: /eap givemoney [gracz] [kwota]");
					return true;
				}
				if (args.length == 2) {
					sender.sendMessage(String.format("Użyj: /eap givemoney %s [kwota]", args[1]));
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target == null) {
					sender.sendMessage("Gracz nie jest online!");
					return true;
				}
				double money = 0.0;
				try {
					money = Double.parseDouble(args[2]);
				} catch (NumberFormatException e) {
					sender.sendMessage("Nieprawidłowa kwota (wartość nie jest cyfrą).");
					return true;
				}
				EternalAdventurePlugin.getEconomy().depositPlayer(target, money);
				String message = String.format("&a&l+ &e&l$%1.2f", money);
				target.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
				print.debug(String.format("Gracz %s otrzymuje kwote %1.2f. Saldo: %1.2f", target.getName(), money, EternalAdventurePlugin.getEconomy().getBalance(target)));
				break;
			}
			case "clearbans": {
				if (sender instanceof Player) {
					sender.sendMessage("Komenda dostępna tylko z konsoli.");
					return true;
				}
				gVar.isBanned.clear();
				sender.sendMessage("Wyczyszczone bany...");
				break;
			}
			case "reloadbans": {
				if (sender instanceof Player) {
					sender.sendMessage("Komenda dostępna tylko z konsoli.");
					return true;
				}
				sender.sendMessage("Lista banów została przeładowana.");
				PunishmentSystem.reloadBans();
				break;
			}
			case "syncplayedtime": {
				if (sender instanceof Player) {
					if (!sender.hasPermission("eadventureplugin.syncplayedtime")) {
						sender.sendMessage("Brak uprawnień.");
						return true;
					}
				}
				if (args.length == 1) {
					sender.sendMessage("Użyj: /eap syncplayedtime [gracz]");
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target == null) {
					sender.sendMessage("Gracz nie jest online!");
					return true;
				}
				int[] timeForPlayerTime = PlayerUtils.getTimePlayedFromStatistic(target);
				PlayerData pd = PlayerData.get(target);
				pd.onlineHours = timeForPlayerTime[0];
				pd.onlineMinutes = timeForPlayerTime[1];
				pd.onlineSeconds = timeForPlayerTime[2];
				MySQLStorage storage = EternalAdventurePlugin.getMySQL();
				if (pd.dbid != 0)
					storage.execute(String.format("UPDATE `players` SET `onlineHours`='%d', `onlineMinutes`='%d', `onlineSeconds`='%d', `maxSessionOnlineSeconds`='%d' WHERE `id`='%d';", pd.onlineHours, pd.onlineMinutes, pd.onlineSeconds, pd.maxSessionOnlineSeconds, pd.dbid));
				sender.sendMessage("Zsynchronizowano przegrany czas gry ze statystyk Bukkita dla gracza: " + target.getName());
				break;
			}
			case "addcustomitem": {
				if (sender instanceof Player player) {
					if (player.hasPermission("eadventureplugin.cmd.addcustomitem")) {
						if (args.length == 1) {
							sender.sendMessage("Użyj: /eap addcustomitem [nazwapliku]");
							return true;
						}
						ItemStack heldItem = player.getInventory().getItemInMainHand();
						Utils.saveItemStackToFile(heldItem, args[1]);
						sender.sendMessage("Zapisano CustomItem: " + heldItem.getItemMeta().getDisplayName());
					}
				}
				break;
			}
			case "getcustomitem": {
				if (sender instanceof Player player) {
					if (args.length == 1) {
						sender.sendMessage("Użyj: /eap getcustomitem [nazwapliku]");
						return true;
					}
					ItemStack customItem = gVar.customItems.get(args[1]);
					if (customItem == null) {
						sender.sendMessage("Nie znaleziono przedmiotu.");
						return true;
					}
					player.getInventory().addItem(customItem);
					sender.sendMessage("Gotowe! Dodano do EQ: " + customItem.getItemMeta().getDisplayName());
				}
				break;
			}
			case "armorfixreload": {
				gVar.colorIssueResolverIA.reloadConfig();
				sender.sendMessage("Wykonano reload configu obiektu ColorIssueResolverIA - więcej info w konsoli :)");
				break;
			}
			case "plist": {
				sender.sendMessage(Utils.color("&4&l----------------------------------------"));
				sender.sendMessage(Utils.color("&aLista graczy online:"));
				sender.sendMessage(" ");
				int lp = 1;
				for (Player p : Bukkit.getOnlinePlayers()) {
					String playerIP = p.getAddress().getAddress().getHostAddress();
					String playerListEntry = String.format("&e%d&f. &2&l%s &f(Ping: &a&l%dms&f) (IP: &7&l%s&f)", lp, p.getName(), p.getPing(), playerIP);
					sender.sendMessage(Utils.color(playerListEntry));
					lp++;
				}
				if (lp == 1) {
					sender.sendMessage("-brak graczy online-");
				}
				sender.sendMessage(" ");
				sender.sendMessage(Utils.color("&4&l----------------------------------------"));
				break;
			}
			case "fabric": {
				if (gVar.antiBot) {
					gVar.antiBot = false;
					sender.sendMessage("antiBot: DISABLED!");
				} else {
					gVar.antiBot = true;
					sender.sendMessage("antiBot: ENABLED!");
				}
				break;
			}
			case "rcl": {
				RegionCommandLooper.reload();
				sender.sendMessage("Przeładowano config RegionCommandLooper!");
				/*if (args.length == 1) {
					sender.sendMessage("Użyj: /eap rcl [set/del] [regionName] [reLoopSeconds] [command]");
					return true;
				}
				String option = args[1];
				if (!option.equalsIgnoreCase("set") && !option.equalsIgnoreCase("del") && !option.equalsIgnoreCase("reload")) {
					sender.sendMessage("Użyj: /eap rcl [set/del] [regionName] [reLoopSeconds] [command]");
					return true;
				}
				if (option.equalsIgnoreCase("reload")) {
					RegionCommandLooper.reload();
					sender.sendMessage("Przeładowano config RegionCommandLooper!");
					return true;
				}
				if (args.length == 2) {
					sender.sendMessage(String.format("Użyj: /eap rcl %s [regionName] [reLoopSeconds] [command]", option));
					return true;
				}
				String regionName = args[2];
				if (args.length == 3) {
					sender.sendMessage(String.format("Użyj: /eap rcl %s %s [reLoopSeconds] [command]", option, regionName));
					return true;
				}
				int seconds = Utils.isNumber(args[3]);
				if (seconds < 0) {
					sender.sendMessage("Niepoprawna ilość sekund");
					return true;
				}
				if (args.length == 4) {
					sender.sendMessage(String.format("Użyj: /eap rcl %s %s %d [command]", option, regionName, seconds));
					return true;
				}
				StringBuilder reasonBuilder = new StringBuilder();
				for (int i = 4; i < args.length; i++) {
					reasonBuilder.append(args[i]).append(" ");
				}
				String command = reasonBuilder.toString().trim(); // Remove spaces
				print.debug(String.format("%s %s %d %s", option, regionName, seconds, command));
				if (option.equalsIgnoreCase("set")) {
					RegionCommandLooper.set(regionName, command, seconds);
					sender.sendMessage("Ustawiono nowy wpis dla RegionCommandLooper!");
					return true;
				}
*/
				return true;
			}
			default: {
				usage(sender);
			}

		}
		/*
		 * if (sender instanceof Player) { Player player = (Player) sender;
		 * player.sendMessage("Komenda wykonana!"); }
		 */
		return true;
	}

	private boolean usage(CommandSender s) {
		s.sendMessage("Użyj: /eap [argument]");
		return true;
	}
}