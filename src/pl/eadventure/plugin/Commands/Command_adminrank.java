package pl.eadventure.plugin.Commands;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Command_adminrank implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		//Czas online, czas online AFK, komendy: kartoteka, banlist, god, fly, alts, whois, acprofil, logs, spec, survival
		//Postawione bloki, zniszczone bloki, zabite moby, zabici gracze.
		if (args.length < 1) {
			Utils.commandUsageMessage(sender, "/adminrank [ilość dni wstecz]");
			return true;
		}
		int paramDaysAgo = Utils.stringToInt(args[0]);
		if (paramDaysAgo < 1 || paramDaysAgo > 90) paramDaysAgo = 30;
		//kick, ban, warn, mute
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		final List<String> staffGroups = Arrays.asList("admin", "gamemaster", "moderator", "support");
		LuckPerms luckPerms = LuckPermsProvider.get();
		UserManager userManager = luckPerms.getUserManager();
		CompletableFuture<Set<UUID>> allUsersFuture = userManager.getUniqueUsers();
		int finalParamDaysAgo = paramDaysAgo;
		allUsersFuture.thenAcceptAsync(uuids -> {
			Map<String, List<String>> groupedStaff = new HashMap<>();
			for (String group : staffGroups) {
				groupedStaff.put(group, new ArrayList<>());
			}

			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (UUID uuid : uuids) {
				CompletableFuture<User> userFuture = userManager.loadUser(uuid);
				futures.add(userFuture.thenAccept(user -> {
					String group = user.getPrimaryGroup().toLowerCase();
					if (staffGroups.contains(group)) {
						String name = user.getUsername();
						if (name != null) {
							groupedStaff.get(group).add(name);
						}
					}
				}));
			}

			// Po załadowaniu wszystkich użytkowników
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
				boolean anyFound = false;
				HashMap<String, String> finalStaffList = new HashMap<>();

				for (String group : staffGroups) {
					List<String> names = groupedStaff.get(group);
					if (!names.isEmpty()) {
						anyFound = true;
						String groupName = group.substring(0, 1).toUpperCase() + group.substring(1);
						//sender.sendMessage("§e" + groupName + ":");
						//names.forEach(name -> sender.sendMessage(" §7- §b" + name));
						names.forEach(name -> finalStaffList.put(name, groupName));
					}
				}
				if (!anyFound) {
					sender.sendMessage("§cBrak członków zespołu w bazie.");
				}

				// Budowanie dynamicznego zapytania SQL
				StringBuilder placeholders = new StringBuilder();
				ArrayList<Object> parameters = new ArrayList<>();
				parameters.add(finalParamDaysAgo); // na końcu dodajemy wartość dla INTERVAL ? DAY
				for (String nick : finalStaffList.keySet()) {
					if (placeholders.length() > 0) placeholders.append(", ");
					//sender.sendMessage(Utils.mm(getTag(finalStaffList.get(nick)) + nick));
					placeholders.append("?");
					parameters.add(nick);
				}


				String sql = "SELECT p.nick, " +
						"SUM(TIMESTAMPDIFF(SECOND, s.start, s.end)) AS total_time_online_seconds, " +
						"SUM(s.afk) AS total_time_afk_seconds, " +
						"(SUM(TIMESTAMPDIFF(SECOND, s.start, s.end)) - SUM(s.afk)) AS total_active_time_seconds " +
						"FROM players p " +
						"LEFT JOIN sessions s ON s.uid = p.id AND s.start >= NOW() - INTERVAL ? DAY " +
						"WHERE p.nick IN (" + placeholders.toString() + ") " +
						"GROUP BY p.nick " +
						"ORDER BY total_active_time_seconds DESC;";

				print.debug(MySQLStorage.getDebugSQL(sql, parameters));
				sender.sendMessage(Utils.mm(" "));
				sender.sendMessage(Utils.mm("<bold><white>---------------------------------------------"));
				sender.sendMessage(Utils.mm(" "));
				sender.sendMessage(Utils.mm("<bold><green>Statystyki członków zespołu (ostatnie " + finalParamDaysAgo + " dni):"));
				sender.sendMessage(Utils.mm(" "));
				storage.querySafe(sql, parameters, queryResult -> {
					int numRows = (int) queryResult.get("num_rows");
					ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
					if (numRows > 0) {
						for (int i = 0; i < numRows; i++) {
							String nick = (String) rows.get(i).get("nick");
							BigDecimal total_time_online = (BigDecimal) rows.get(i).get("total_time_online_seconds");
							if (total_time_online == null) total_time_online = BigDecimal.valueOf(0);
							int total_time_online_sec = total_time_online.intValue();

							BigDecimal total_time_afk = (BigDecimal) rows.get(i).get("total_time_afk_seconds");
							if (total_time_afk == null) total_time_afk = BigDecimal.valueOf(0);
							int total_time_afk_sec = total_time_afk.intValue();

							BigDecimal total_active_time = (BigDecimal) rows.get(i).get("total_active_time_seconds");
							if (total_active_time == null) total_active_time = BigDecimal.valueOf(0);
							int total_active_time_sec = total_active_time.intValue();

							//convert
							int[] timeOnline = Utils.convertSecondsToTime(total_time_online_sec);
							String total_time_online_formatted = String.format("%d:%02d:%02d", timeOnline[0], timeOnline[1], timeOnline[2]);

							int[] timeAfk = Utils.convertSecondsToTime(total_time_afk_sec);
							String total_time_afk_formatted = String.format("%d:%02d:%02d", timeAfk[0], timeAfk[1], timeAfk[2]);

							int[] timeActive = Utils.convertSecondsToTime(total_active_time_sec);
							String total_active_time_formatted = String.format("%d:%02d:%02d", timeActive[0], timeActive[1], timeActive[2]);

							int avg_active_time_sec = total_active_time_sec / finalParamDaysAgo;
							int[] avgActiveTime = Utils.convertSecondsToTime(avg_active_time_sec);
							String avg_active_time_formatted = String.format("%d:%02d:%02d", avgActiveTime[0], avgActiveTime[1], avgActiveTime[2]);

							// final format
							String final_str = String.format(
									"<white>%d<grey>. <yellow><bold>%s%s</bold> <grey>[Suma: <white>%s</white>] [AFK: <white>%s</white>] [Aktyw.: <green><bold>%s</bold></green>] [Śr./dzień: <blue>%s</blue>]",
									i + 1,
									getTag(finalStaffList.get(nick.toLowerCase())),
									nick,
									total_time_online_formatted,
									total_time_afk_formatted,
									total_active_time_formatted,
									avg_active_time_formatted
							);
							sender.sendMessage(Utils.mm(final_str));
						}
						sender.sendMessage(Utils.mm(" "));
						sender.sendMessage(Utils.mm("<bold><white>---------------------------------------------"));
						sender.sendMessage(Utils.mm(" "));
					}
				});


			});
		});
		return true;
	}

	public static String getTag(String fullName) {
		if (fullName == null) return "null";
		String result = "[?]";
		switch (fullName.toLowerCase()) {
			case "admin" -> result = "<grey>[<#FF0000>A</#FF0000>]</grey>";
			case "moderator" -> result = "<grey>[<#FF8000>M</#FF8000>]</grey>";
			case "gamemaster" -> result = "<grey>[<dark_blue>GM</dark_blue>]</grey>";
			case "support" -> result = "<grey>[<aqua>S</aqua>]</grey>";
		}
		return result;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
