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

public class Command_msgtog implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.disabledMsg) {
				pd.disabledMsg = false;
				sender.sendMessage(Utils.mm("<gray>Otrzymywanie prywatnych wiadomości <green>włączone<gray>."));
			} else {
				pd.disabledMsg = true;
				sender.sendMessage(Utils.mm("<gray>Otrzymywanie prywatnych wiadomości <red>wyłączone<grey>."));
			}
			MySQLStorage storage = EternalAdventurePlugin.getMySQL();
			String sql = "UPDATE players SET disabledMsg=? WHERE id=?";
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add((int) (pd.disabledMsg ? 1 : 0));
			parameters.add(pd.dbid);
			storage.executeSafe(sql, parameters);
		}
		return true;
	}
}
