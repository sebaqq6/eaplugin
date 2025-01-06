package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
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
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
		if (commandSender instanceof Player player) {
			if (args.length == 1) {
				if (!player.hasPermission("eadventureplugin.creative.cangive")) {
					commandSender.sendMessage(Utils.mm("<#FF0000>Nie posiadasz uprawnień aby nadawać innym graczom creative."));
					return true;
				}
				String targetNick = args[0];
				Player targetPlayer = Bukkit.getPlayer(targetNick);
				if (targetPlayer == null || !targetPlayer.isOnline()) {
					commandSender.sendMessage(Utils.mm("<#FF0000>" + targetNick + " jest <b>offline</b>.</#FF0000>"));
					return true;
				}
				switch (toggleCreative(targetPlayer)) {
					case -2 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>" + targetPlayer.getName() + " nie ma pustego <b>ekwipunku</b>.</#FF0000>"));
					}
					case -1 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>" + targetNick + " jest <b>offline</b>.</#FF0000>"));
					}
					case 0 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>Tryb kreatywny dla " + targetPlayer.getName() + " został <b>wyłączony</b>.</#FF0000>"));
					}
					case 1 -> {
						commandSender.sendMessage(Utils.mm("<#00FF00>Tryb kreatywny dla " + targetPlayer.getName() + " został <b>aktywowany</b>.</#00FF00>"));
					}
				}
			} else {
				toggleCreative(player);
			}
		} else {
			if (args.length == 1) {
				String targetNick = args[0];
				Player targetPlayer = Bukkit.getPlayer(targetNick);
				if (targetPlayer == null || !targetPlayer.isOnline()) {
					commandSender.sendMessage(Utils.mm("<#FF0000>" + targetNick + " jest <b>offline</b>.</#FF0000>"));
					return true;
				}
				switch (toggleCreative(targetPlayer)) {
					case -2 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>" + targetPlayer.getName() + " nie ma pustego <b>ekwipunku</b>.</#FF0000>"));
					}
					case -1 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>" + targetNick + " jest <b>offline</b>.</#FF0000>"));
					}
					case 0 -> {
						commandSender.sendMessage(Utils.mm("<#FF0000>Tryb kreatywny dla " + targetPlayer.getName() + " został <b>wyłączony</b>.</#FF0000>"));
					}
					case 1 -> {
						commandSender.sendMessage(Utils.mm("<#00FF00>Tryb kreatywny dla " + targetPlayer.getName() + " został <b>aktywowany</b>.</#00FF00>"));
					}
				}
			} else {
				commandSender.sendMessage("Komenda dostępna tylko z poziomu gry, z wyjątkiem /creative [nick]");
			}
		}
		return true;
	}

	private int toggleCreative(Player player) {
		if (!player.isOnline()) return -1;
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
			return 0;
		} else { //enable creative mode
			//temp disable backup inventory, player we need empty inventory
			if ((PlayerUtils.getInventoryItemsCount(player) != 0 || PlayerUtils.getArmorItemsCount(player) != 0) &&
					!player.hasPermission("eadventureplugin.creative.bypass")) {
				player.sendMessage(Utils.mm("<#888888>Wymagany jest <u>pusty</u> ekwipunek!</#888888>"));
				return -2;
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
			return 1;
		}
	}
}
