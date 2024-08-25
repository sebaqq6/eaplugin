package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.HomesInterface;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Command_dzialka implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if(sender instanceof Player player) {
			HomesInterface hi = new HomesInterface();
			hi.loadFromPlayer(player);
			hi.printDebugData();
			if (args.length == 0) {
				hi.renderMainMenuGUI(player);
				PlayerData pd = PlayerData.get(player);
				pd.homesInterface = hi;
			} else if(args.length == 1) {
				if(!hi.teleportToCuboidByName(player, args[0])) {
					PlayerUtils.sendColorMessage(player, "&7Nie masz żadnych uprawnień do tej działki lub działka nie istnieje.");
				}
			}
		}
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {//cuboid name
			if(sender instanceof Player player) {
				PlayerData pd = PlayerData.get(player);
				HomesInterface hi = pd.homesInterface;
				if(hi == null) {
					hi = new HomesInterface();
					hi.loadFromPlayer(player);
					pd.homesInterface = hi;
				}
				return StringUtil.copyPartialMatches(args[0], hi.getAllCuboidsList(), new ArrayList<>());
			}
		}
		return Collections.emptyList();
	}
}
