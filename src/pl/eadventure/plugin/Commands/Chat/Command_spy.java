package pl.eadventure.plugin.Commands.Chat;

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

public class Command_spy implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.enabledSpy) {
				pd.enabledSpy = false;
				sender.sendMessage(Utils.mm("<gold>Śledzenie msg <red>wyłączone<gold>."));
			} else {
				pd.enabledSpy = true;
				sender.sendMessage(Utils.mm("<gold>Śledzenie msg <green>włączone<gold>w."));
			}
			MySQLStorage storage = EternalAdventurePlugin.getMySQL();
			String sql = "UPDATE players SET enabledSpy=? WHERE id=?";
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add((int) (pd.enabledSpy ? 1 : 0));
			parameters.add(pd.dbid);
			storage.executeSafe(sql, parameters);
		}
		return true;
	}
}
