package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.Modules.Chat.IgnoreList;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// /ingoruj, /ignorujmsg, /ignorowani
public class Command_ignore implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (args.length == 0) {//get target nick args[0]
				//show menu
				pd.ignoreList.showGui(player, 1);
				return true;
			}
			String targetName = args[0];
			if (!PunishmentSystem.getListPlayersAll().contains(targetName)) {
				sender.sendMessage(Utils.color("&7Taki gracz nie istnieje."));
				return true;
			}
			//add/remove player
			if (pd.ignoreList.add(targetName, IgnoreList.EntryType.ALL)) {
				sender.sendMessage(Utils.mm("<grey>Dodano do ignorowanych gracza:<red> " + targetName));
			} else if (pd.ignoreList.remove(targetName)) {
				sender.sendMessage(Utils.mm("<grey>Usunięto z ignorowanych gracza:<green> " + targetName));
			} else {
				sender.sendMessage("<grey>Wystąpił nieoczekiwany błąd.");
			}
		}
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//nick
			if (args[0].isEmpty()) return List.of("Wpisz pierwszą literę lub wciśnij enter aby wyświetlić listę...");
			return StringUtil.copyPartialMatches(args[0], PunishmentSystem.getListPlayersAll(), new ArrayList<>());
		} else
			return Collections.emptyList();
	}
}
