package pl.eadventure.plugin.Commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.PlayerUtils;
import pl.eadventure.plugin.Utils.Utils;

public class Command_creative implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		if (commandSender instanceof Player player) {
			PlayerData pd = PlayerData.get(player);
			if (pd.creativeMode) {//disable creative mode
				pd.creativeMode = false;
				player.getInventory().clear();
				player.getInventory().setHelmet(null);
				player.getInventory().setChestplate(null);
				player.getInventory().setLeggings(null);
				player.getInventory().setBoots(null);
				player.getInventory().setContents(pd.itemsBackupCreative);
				player.getInventory().setArmorContents(pd.armorBackupCreative);
				player.setGameMode(GameMode.SURVIVAL);
				player.sendMessage(Utils.mm("<#FF0000>Tryb kreatywny <b>wyłączony</b>.</#FF0000>"));

			} else { //enable creative mode
				//temp disable backup inventory, player we need empty inventory
				if ((PlayerUtils.getInventoryItemsCount(player) != 0 || PlayerUtils.getArmorItemsCount(player) != 0) &&
						!player.hasPermission("eadventureplugin.creative.bypass")) {
					player.sendMessage(Utils.mm("<#888888>Musisz mieć <u>pusty</u> ekwipunek!</#888888>"));
					return true;
				}
				pd.itemsBackupCreative = player.getInventory().getContents();
				pd.armorBackupCreative = player.getInventory().getArmorContents();
				player.getInventory().clear();
				player.getInventory().setHelmet(null);
				player.getInventory().setChestplate(null);
				player.getInventory().setLeggings(null);
				player.getInventory().setBoots(null);
				pd.creativeLastBreakPos = null;
				pd.creativeLastPlacedPos = null;
				pd.creativeMode = true;
				player.setGameMode(GameMode.CREATIVE);
				player.sendMessage(Utils.mm("<#00FF00>Tryb kreatywny <b>aktywowany</b>.</#00FF00>"));
			}

		} else {
			commandSender.sendMessage("Komenda dostępna tylko z poziomu gry.");
		}
		return true;
	}
}
