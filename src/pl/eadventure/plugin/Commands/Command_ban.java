package pl.eadventure.plugin.Commands;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PunishmentSystem;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Command_ban implements TabExecutor {
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (args.length == 1) {//nick
			if(args[0].isEmpty()) return List.of("Wpisz pierwszą literę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersCanBeBanned(), new ArrayList<>());
		} else if (args.length == 2) {//time/perm
			List<String> cmdlist = Arrays.asList("perm", "30m", "2h", "1d", "7d", "30d", "60d", "120d", "1d,12h",
					"1d,12h,30m", "1d,30m");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		} else if (args.length == 3) {//ban type
			List<String> cmdlist = Arrays.asList("ip", "konto");
			return StringUtil.copyPartialMatches(args[2], cmdlist, new ArrayList<>());
		} else if (args.length == 4) {//reason
			List<String> cmdlist = Arrays.asList("Cheater.", "Szkodnik.", "Griefing.", "Reklama.", "Nieprzestrzeganie regulaminu.");
			return StringUtil.copyPartialMatches(args[3], cmdlist, new ArrayList<>());
		}
		else
			return Collections.emptyList();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		new BukkitRunnable() {
			@Override
			public void run() {
				// /ban <nick> <czas/perm> <rodzaj bana/ip,nick,uuid,all> <powód>
				if (args.length == 0) {//get target nick args[0]
					Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [perm/czas] [typ bana] [powód]", label));
					return;
				}
				String targetName = args[0];
				if(targetName.equalsIgnoreCase(sender.getName())) {//if target == player
					sender.sendMessage(Utils.color("&7Nie możesz tego użyć na sobie."));
					return;
				}
				if(sender instanceof Player player)
				{
					if(PlayerUtils.isAdminPermissionHasHigher(player.getName(), targetName)) {
						sender.sendMessage(Utils.color("&7Ten gracz ma wyższe uprawnienia administracyjne niż Ty."));
						return;
					}
				}
				if (args.length == 1) {//time/perm args[1]
					//Utils.commandUsageMessage(sender, String.format("/%s %s [perm/czas] [typ bana] [powód]", label, targetName));
					String[] newArgs = {targetName, "perm", "ip", "Nieprzestrzeganie regulaminu."};
					onCommand(sender, cmd, label, newArgs);
					return;
				}
				String timeBanParam = args[1];
				long timeExpireBanMinutes = -2;
				if (timeBanParam.equalsIgnoreCase("perm")) timeExpireBanMinutes = -1;
				else {
					String[] time = timeBanParam.split(",");
					int timeDays = 0, timeHours = 0, timeMinutes = 0;

					switch (time.length) {
						case 1:
							// Obsługa jednej części
							if (time[0].contains("d")) {
								timeDays = Integer.parseInt(time[0].replaceAll("\\D", ""));
							} else if (time[0].contains("h")) {
								timeHours = Integer.parseInt(time[0].replaceAll("\\D", ""));
							} else if (time[0].contains("m")) {
								timeMinutes = Integer.parseInt(time[0].replaceAll("\\D", ""));
							}
							break;
						case 2:
							// Obsługa dwóch części
							for (String part : time) {
								if (part.contains("d")) {
									timeDays = Integer.parseInt(part.replaceAll("\\D", ""));
								} else if (part.contains("h")) {
									timeHours = Integer.parseInt(part.replaceAll("\\D", ""));
								} else if (part.contains("m")) {
									timeMinutes = Integer.parseInt(part.replaceAll("\\D", ""));
								}
							}
							break;
						case 3:
							// Obsługa wszystkich trzech części
							timeDays = Integer.parseInt(time[0].replaceAll("\\D", ""));
							timeHours = Integer.parseInt(time[1].replaceAll("\\D", ""));
							timeMinutes = Integer.parseInt(time[2].replaceAll("\\D", ""));
							break;
						default:
							//obsługa błędu
							sender.sendMessage(Utils.color("&7Niepoprawnie podany czas."));
							return;
					}

					if (timeDays == 0 && timeHours == 0 && timeMinutes == 0) {
						sender.sendMessage(Utils.color("&7Niepoprawnie podany czas."));
						return;
					}
					int timeDaysToMinutes = timeDays * 60 * 24;
					int timeHoursToMinutes = timeHours * 60;
					timeExpireBanMinutes = timeDaysToMinutes + timeHoursToMinutes + timeMinutes;
				}
				if (args.length == 2) {//ban type args[2]
					Utils.commandUsageMessage(sender, String.format("/%s %s %s [typ bana] [powód]", label, targetName, timeBanParam));
					return;
				}
				String banTypeParams = args[2];
				int bantype = -1;
				switch (banTypeParams.toLowerCase()) {
					case "ip":
						bantype = PunishmentSystem.BanType.NICK_UUID_IP;
						break;
					case "konto":
						bantype = PunishmentSystem.BanType.NICK_UUID;
						break;
					/*case "nick":
						bantype = PunishmentSystem.BanType.NICK;
						break;*/
					default:
						sender.sendMessage(Utils.color("&7Nieprawidłowy typ bana."));
						return;
				}
				if (args.length == 3) {//ban type args[2]
					Utils.commandUsageMessage(sender, String.format("/%s %s %s %s [powód]", label, targetName, timeBanParam, banTypeParams));
					return;
				}
				//Join all arguments
				StringBuilder reasonBuilder = new StringBuilder();
				for (int i = 3; i < args.length; i++) {
					reasonBuilder.append(args[i]).append(" ");
				}
				String reason = reasonBuilder.toString().trim(); // Remove spaces


				if (reason.length() < 2) {
					sender.sendMessage(Utils.color("&7Powód jest za krótki."));
					return;
				}
				if (reason.length() > 50) {
					sender.sendMessage(Utils.color("&7Powód jest za długi."));
					return;
				}
				if (PunishmentSystem.getListPlayersCanBeBanned().contains(targetName)) {
					PunishmentSystem.notifyMessage(PunishmentSystem.LogType.BAN, targetName, sender.getName(), reason, timeExpireBanMinutes);
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7został pomyślnie zbanowany.", targetName)));
					PunishmentSystem.banPlayer(targetName, sender.getName(), (int) timeExpireBanMinutes, bantype, reason);
				} else if (PunishmentSystem.getListPlayersCanBeUnbanned().contains(targetName)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7jest już zbanowany.", targetName)));
				} else {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7nie istnieje.", targetName)));
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
		return true;
	}
}
