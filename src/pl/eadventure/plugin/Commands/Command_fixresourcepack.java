package pl.eadventure.plugin.Commands;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.List;

public class Command_fixresourcepack implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		//ItemsAdder.applyResourcepack();
		if (args.length < 1) {
			Utils.commandUsageMessage(sender, "/fixresourcepack [nick]");
			return true;
		}
		String nick = args[0];
		Player target = Bukkit.getPlayer(nick);
		if (target == null || !target.isOnline()) {
			sender.sendMessage(Utils.mm("<red>Ten gracz jest offline."));
			return true;
		}
		sender.sendMessage(Utils.mm(String.format("<#00FF00>Próba naprawy ResourcePack dla gracza <bold>%s", target.getName())));
		sender.sendMessage(Utils.mm("<#999999>Odczekaj chwile (2-3 min) zanim spróbujesz ponownie. Spytaj gracza, czy pomogło."));
		ItemsAdder.applyResourcepack(target);
		print.error(String.format("%s zlecił próbę naprawy ResourcePack dla %s", sender.getName(), target.getName()));
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
