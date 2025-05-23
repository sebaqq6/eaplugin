package pl.eadventure.plugin.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

import java.util.*;

//&5&l༺&5&m-----&5&l༻ &a&lSTART /ROLL &5&l༺&5&m-----&5&l༻
//&5&l༺&5&m-----&5&l༻ &c&lKONIEC /ROLL &5&l༺&5&m-----&5&l༻
//&5&l༺&5&m-----&5&l༻ &e&lWYGRYWA: &a<nick> &5- &e<wynik> &5&l༺&5&m-----&5&l༻
//&8&l[&e&lROLL&8&l] &2%player% &7wylosował/a liczbę&e&l 1 &8(1-100)&7.
public class Command_roll implements TabExecutor {
	private final Map<UUID, Long> cooldowns = new HashMap<>();
	private static final int COOLDOWN_SECONDS = 20;

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			//cooldown check
			UUID playerUUID = player.getUniqueId();
			long currentTime = System.currentTimeMillis();

			if (cooldowns.containsKey(playerUUID)) {
				long lastUsed = cooldowns.get(playerUUID);
				long timePassed = (currentTime - lastUsed) / 1000;
				if (timePassed < COOLDOWN_SECONDS) {
					int timeLeft = COOLDOWN_SECONDS - (int) timePassed;
					player.sendMessage(Utils.mm("<yellow>⏳ <grey>Możesz użyć tej komendy ponownie za <yellow>" + timeLeft + " sek<grey>."));
					return true;
				}
			}
			cooldowns.put(playerUUID, currentTime);
			//--------------------------------BODY COMMAND
			int randomNumber = new Random().nextInt(100) + 1;
			int randomNumberEx = 95 + new Random().nextInt(6);
			if (player.hasPermission("eadventureplugin.special.roll")) {
				randomNumber = randomNumberEx;
			}
			Component message = Utils.mm(
					"<dark_gray><bold>[</bold></dark_gray><yellow><bold>ROLL</bold></yellow>"
							+ "<dark_gray><bold>]</bold></dark_gray> <dark_green>" + player.getName()
							+ "</dark_green> <gray>wylosował/a liczbę <yellow><bold>" + randomNumber
							+ "</bold></yellow> <dark_gray>(1-100)</dark_gray><gray>.</gray>"
			);
			Location playerLocation = player.getLocation();
			for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
				if (!nearbyPlayer.getWorld().equals(playerLocation.getWorld())) {
					continue;
				}
				if (nearbyPlayer.getLocation().distance(playerLocation) <= 50) {
					nearbyPlayer.sendMessage(message);
					PlayerData pd = PlayerData.get(nearbyPlayer);
					if (pd.rollTool != null && pd.rollTool.isRegisterRolls()) {
						pd.rollTool.addRoll(player.getName(), randomNumber);
					}
					nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1.0f, 1.5f);
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
