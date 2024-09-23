package pl.eadventure.plugin.Commands;

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
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO default cmd 10 min
public class Command_mute implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (args.length == 0) {//get target nick args[0]
					Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [czas] [powód]", label));
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
				if (args.length == 1) {//time args[1]
					Utils.commandUsageMessage(sender, String.format("/%s %s [czas] [powód]", label, targetName));
					return;
				}
				String timeParam = args[1];
				int timeMinutes = Utils.parseTimeToMinutes(timeParam);


				if (timeMinutes == -1) {
					sender.sendMessage(Utils.color("&7Niepoprawnie podany czas."));
					return;
				}
				if (timeMinutes > 24 * 60) {
					sender.sendMessage("&7Nie możesz uciszyć na dłużej niż 24 godziny.");
					return;
				}

				int calculatedExpire = (int) Utils.getUnixTimestamp() + timeMinutes * 60;

				if (args.length == 2) {//reason
					Utils.commandUsageMessage(sender, String.format("/%s %s %s [powód]", label, targetName, timeParam));
					return;
				}
				// Build reason from other args
				StringBuilder reasonBuilder = new StringBuilder();
				for (int i = 2; i < args.length; i++) {//param 2
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
				PunishmentSystem.mutePlayer(targetName, sender, reason, calculatedExpire);
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
		} else if (args.length == 2) {//time
			List<String> cmdlist = Arrays.asList("2m", "5m", "10m", "15m", "30m");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		} else if (args.length == 3) {//reason
			List<String> cmdlist = Arrays.asList("Wulgaryzmy.", "Obraza.", "Spam.", "Reklama.", "Nieprzestrzeganie regulaminu.");
			return StringUtil.copyPartialMatches(args[2], cmdlist, new ArrayList<>());
		} else
			return Collections.emptyList();
	}
}
