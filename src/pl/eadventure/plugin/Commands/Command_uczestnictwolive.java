package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;

public class Command_uczestnictwolive implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.unParticipateLive == 1) {
				pd.unParticipateLive = 0;//enable participate live
				player.sendMessage(Utils.mm("<grey>Uczestnictwo w live zostało <green>włączone<grey>."));
			} else {
				pd.unParticipateLive = 1;//disable participate live
				player.sendMessage(Utils.mm("<grey>Uczestnictwo w live zostało <red>wyłączone<grey>."));
			}
			MySQLStorage storage = EternalAdventurePlugin.getMySQL();
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add(pd.unParticipateLive);
			parameters.add(pd.dbid);
			storage.executeSafe("UPDATE players SET unParticipateLive=? WHERE id=?;", parameters);
		}
		return true;
	}

	public static String placeholder(Player player) {
		PlayerData pd = PlayerData.get(player);
		if (pd.unParticipateLive == 1) return "nie";
		return "tak";
	}
}
