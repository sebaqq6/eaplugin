package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.*;

public class Command_warn implements TabExecutor {
	static HashMap<String, Long> warnCooldown = new HashMap<>();

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		new BukkitRunnable() {//Async
			@Override
			public void run() {
				// /warn [nick] [czas] [powód]
				if (args.length == 0) {//get target nick args[0]
					Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [powód]", label));
					return;
				}
				String targetName = args[0];
				if (targetName.equalsIgnoreCase(sender.getName())) {//if target == player
					sender.sendMessage(Utils.color("&7Nie możesz tego użyć na sobie."));
					return;
				}
				if (!PunishmentSystem.getListPlayersAll().contains(targetName)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7nie istnieje.", targetName)));
					return;
				}
				if (sender instanceof Player player) {
					if (PlayerUtils.isAdminPermissionHasHigher(player.getName(), targetName)) {//lag - use only async thread
						sender.sendMessage(Utils.color("&7Ten gracz ma wyższe uprawnienia administracyjne niż Ty."));
						return;
					}
				}
				if (PunishmentSystem.getListPlayersCanBeUnbanned().contains(targetName)) {
					sender.sendMessage(Utils.color("&7Nie możesz dać ostrzeżenia, gdyż ten gracz jest zbanowany."));
					return;
				}
				//cooldown 10s
				long lastWarn = warnCooldown.getOrDefault(targetName, 0L);
				long lastWarnSecondAgo = Utils.getUnixTimestamp() - lastWarn;
				if (lastWarnSecondAgo <= 10) {
					sender.sendMessage(Utils.color("&7Ten gracz otrzymał chwilę temu ostrzeżenie. Odczekaj " + (10 - lastWarnSecondAgo) + " sekund aby nadać kolejne."));
					return;
				}

				if (args.length == 1) {//time args[1]
					Utils.commandUsageMessage(sender, String.format("/%s %s [powód]", label, targetName));
					return;
				}
				/*String timeParam = args[1];
				int timeMinutes = Utils.parseTimeToMinutes(timeParam);
				if (timeMinutes == -1) {
					sender.sendMessage(Utils.color("&7Niepoprawnie podany czas."));
					return;
				}
				if (args.length == 2) {
					Utils.commandUsageMessage(sender, String.format("/%s %s %s [powód]", label, targetName, timeParam));
					return;
				}*/
				int timeMinutes = Utils.parseTimeToMinutes("7d");
				// Build reason from other args
				StringBuilder reasonBuilder = new StringBuilder();
				for (int i = 1; i < args.length; i++) {//param 2
					reasonBuilder.append(args[i]).append(" ");
				}
				String reason = reasonBuilder.toString().trim(); // Remove redundant spaces

				if (reason.length() < 2) {
					sender.sendMessage(Utils.color("&7Powód jest za krótki."));
					return;
				}
				if (reason.length() > 50) {
					sender.sendMessage(Utils.color("&7Powód jest za długi."));
					return;
				}

				int expireTimestamp = (int) Utils.getUnixTimestamp() + timeMinutes * 60;

				PunishmentSystem.WarnData wd = null;
				Player targetPlayer = Bukkit.getPlayer(targetName);
				if (targetPlayer == null) //player is offline
				{
					wd = new PunishmentSystem.WarnData();
					wd.load(targetName);
					int hardBreak = 0;
					while (true) {//waiting for load
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						if (wd.isLoaded()) break;//data loaded - stop loop
						hardBreak++;
						if (hardBreak > 5) {
							print.error("Nieoczekiwany błąd /warn -> Loading offline WarnData");
							break;
						}
					}
				} else {//player is online
					PlayerData pd = PlayerData.get(targetPlayer);
					wd = pd.warnData;
				}
				if (wd.isLoaded()) {//WarnData is loaded
					if (wd.size() >= PunishmentSystem.WarnData.maxWarns) {
						sender.sendMessage("Ten gracz posiada już maksymalną ilość warnów.");
						return;
					}
					wd.deleteExpired();
					//message to all
					PunishmentSystem.notifyMessage(PunishmentSystem.LogType.WARN, targetName, sender.getName(), reason, expireTimestamp);
					//message to admin
					sender.sendMessage(Utils.color(String.format("&2%s &7został/a &2pomyślnie &7ostrzeżony.", targetName)));
					//add warn
					wd.add(sender.getName(), reason, expireTimestamp);
					//add cooldown
					warnCooldown.put(targetName, Utils.getUnixTimestamp());
					//message to player
					if (targetPlayer != null) {
						targetPlayer.sendMessage(Utils.color("&c&lOtrzymałeś/aś ostrzeżenie."));
						targetPlayer.sendMessage(Utils.color(String.format("&4Powód&8: &7%s", reason)));
						targetPlayer.sendMessage(Utils.color(String.format("&4Masz teraz &2%d/%d &4ostrzeżeń. Sprawdź &7&l/warns", wd.size(), PunishmentSystem.WarnData.maxWarns)));
					}
					//log
					int[] time = Utils.convertSecondsToTimeWithDays(60 * timeMinutes);
					PunishmentSystem.log(PunishmentSystem.LogType.WARN, targetName, sender.getName(), String.format("%s. %dd, %dg, %dm, %ds.", reason, time[0], time[1], time[2], time[3]));
				} else {
					sender.sendMessage("Wystąpił nieoczekiwany błąd (nie załadowano danych) - zgłoś to!");
				}
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//nick
			if (args[0].isEmpty()) return List.of("Wpisz pierwszą literę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersCanBeBanned(), new ArrayList<>());
		} /*else if (args.length == 2) {//time
			List<String> cmdlist = Arrays.asList("30m", "2h", "1d", "7d", "30d", "60d", "120d", "1d,12h",
					"1d,12h,30m", "1d,30m");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		}*/ else if (args.length == 2) {//reason
			List<String> cmdlist = Arrays.asList("Cheater.", "Szkodnik.", "Griefing.", "Reklama.", "Nieprzestrzeganie regulaminu.");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		} else
			return Collections.emptyList();
	}
}
