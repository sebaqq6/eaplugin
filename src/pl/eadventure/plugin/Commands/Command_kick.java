package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.PunishmentSystem;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
//TODO default cmd only nick
public class Command_kick implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {//get target nick args[0]
			Utils.commandUsageMessage(sender, String.format("/%s [nick gracza] [powód]", label));
			return true;
		}
		String targetName = args[0];
		Player targetPlayer = Bukkit.getPlayer(targetName);
		if(targetPlayer == null) {
			sender.sendMessage(Utils.color("&7Ten gracz nie jest online."));
			return true;
		}
		if(targetName.equalsIgnoreCase(sender.getName())) {//if target == player
			sender.sendMessage(Utils.color("&7Nie możesz tego użyć na sobie."));
			return true;
		}
		if(sender instanceof Player player)
		{
			if(PlayerUtils.isAdminPermissionHasHigher(player.getName(), targetName)) {
				sender.sendMessage(Utils.color("&7Ten gracz ma wyższe uprawnienia administracyjne niż Ty."));
				return true;
			}
		}
		if(args.length == 1) {
			Utils.commandUsageMessage(sender, String.format("/%s %s [powód]", label, targetName));
			return true;
		}
		// Build reason from other args
		StringBuilder reasonBuilder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {//param 1
			reasonBuilder.append(args[i]).append(" ");
		}
		String reason = reasonBuilder.toString().trim(); // Remove redundant spaces

		if (reason.length() < 2) {
			sender.sendMessage(Utils.color("&7Powód jest za krótki."));
			return true;
		}
		if (reason.length() > 50) {
			sender.sendMessage(Utils.color("&7Powód jest za długi."));
			return true;
		}
		PunishmentSystem.notifyMessage(PunishmentSystem.LogType.KICK, targetName, sender.getName(), reason, 0);
		PunishmentSystem.log(PunishmentSystem.LogType.KICK, targetName, sender.getName(), reason);

		reason = reason.replace("-s", "");//remove -s from kickMessage per player
		reason = reason.trim();//remove spaces from beginnig
		String kickMessage = "&7Kick\n" +
				"&7\n" +
				String.format("&cNadał/a: &3%s\n", sender.getName()) +
				String.format("&cPowód: &7%s\n", reason) +
				"&7";
		targetPlayer.kickPlayer(Utils.color(kickMessage));
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &c%s &7został pomyślnie wyrzucony.", targetName)));
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//nick
			return null;
		} else if(args.length == 2) {//reason
			List<String> cmdlist = Arrays.asList("Cheater.", "Szkodnik.", "Griefing.", "Reklama.", "Nieprzestrzeganie regulaminu.");
			return StringUtil.copyPartialMatches(args[1], cmdlist, new ArrayList<>());
		}
		return Collections.emptyList();
	}
}
