package pl.eadventure.plugin.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

import java.util.List;
import java.util.Random;

//&5&l༺&5&m-----&5&l༻ &a&lSTART /ROLL &5&l༺&5&m-----&5&l༻
//&5&l༺&5&m-----&5&l༻ &c&lKONIEC /ROLL &5&l༺&5&m-----&5&l༻
//&5&l༺&5&m-----&5&l༻ &e&lWYGRYWA: &a<nick> &5- &e<wynik> &5&l༺&5&m-----&5&l༻
//&8&l[&e&lROLL&8&l] &2%player% &7wylosował/a liczbę&e&l 1 &8(1-100)&7.
public class Command_roll implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			int randomNumber = new Random().nextInt(100) + 1;
			Component message = Utils.mm(
					"<dark_gray><bold>[</bold></dark_gray><yellow><bold>ROLL</bold></yellow>"
							+ "<dark_gray><bold>]</bold></dark_gray> <dark_green>" + player.getName()
							+ "</dark_green> <gray>wylosował/a liczbę <yellow><bold>" + randomNumber
							+ "</bold></yellow> <dark_gray>(1-100)</dark_gray><gray>.</gray>"
			);
			Location playerLocation = player.getLocation();
			for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
				if (nearbyPlayer.getLocation().distance(playerLocation) <= 50) {
					nearbyPlayer.sendMessage(message);
					PlayerData pd = PlayerData.get(nearbyPlayer);
					if (pd.rollTool != null && pd.rollTool.isRegisterRolls()) {
						pd.rollTool.addRoll(player.getName(), randomNumber);
						pd.rollTool.addRoll("Gracz2", randomNumber);
					}
				}
			}
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
