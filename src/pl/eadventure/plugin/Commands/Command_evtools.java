package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.*;
import java.util.stream.Collectors;

public class Command_evtools implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0)
			return usage(sender);
		switch (args[0].toLowerCase()) {
			case "randomplayer" -> {
				ArrayList<Player> playersInEventWorld = new ArrayList<Player>();
				for (Player players : Bukkit.getOnlinePlayers()) {
					if (isPlayerInEvent(players, sender))
						playersInEventWorld.add(players);
				}
				if (playersInEventWorld.size() == 0) {
					sender.sendMessage(Utils.color("&7Brak graczy na evencie!"));
				} else {
					int getRandomPlayer = new Random().nextInt(playersInEventWorld.size());
					Player randomizedPlayer = playersInEventWorld.get(getRandomPlayer);
					// timer glow
					if (!randomizedPlayer.isGlowing()) {
						print.debug("Podświetlanie gracza: " + randomizedPlayer.getName());
						randomizedPlayer.setGlowing(true);
						Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(EternalAdventurePlugin.getInstance(),
								new Runnable() {
									@Override
									public void run() {
										print.debug("Deaktywacja podświetlenia gracza...");
										if (randomizedPlayer.isOnline()) randomizedPlayer.setGlowing(false);
									}
								}, 200L);// 60 L == 3 sec, 20 ticks == 1 sec
					}
					// timer end
					for (Player players : Bukkit.getOnlinePlayers()) {
						if (isPlayerInEvent(players, sender))
							players.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lWylosowany gracz: &a&l" + randomizedPlayer.getName()));
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lWylosowany gracz: &a&l" + randomizedPlayer.getName()));
				}
			}
			case "countplayers" -> {
				ArrayList<Player> playersInEventWorld = new ArrayList<Player>();
				for (Player players : Bukkit.getOnlinePlayers()) {
					if (isPlayerInEvent(players, sender))
						playersInEventWorld.add(players);
				}
				String message = "&7&lW evencie uczestniczy &a&l" + playersInEventWorld.size() + " &7&lgraczy.";
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
			}
			case "teamplayers" -> {
				if (args.length == 1) {
					sender.sendMessage(Utils.color("&7Użyj: /evtools ev_teamsplayer [ilość drużyn]"));
					return true;
				}
				Integer countTeams;

				try {
					countTeams = Integer.valueOf(args[1]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.color("&7Nieprawidłowa ilość drużyn (wartość nie jest cyfrą)."));
					return true;
				}

				if (countTeams <= 1 || countTeams > 4) {
					sender.sendMessage(Utils.color("&7Prawidłowa ilość drużyn: 2, 3, 4."));
					return true;
				}

				/*ScoreboardManager manager = Bukkit.getScoreboardManager();
				Scoreboard scoreboard = manager.getMainScoreboard();
				Team teamRed = scoreboard.getTeam("druzynaczerwona");
				Team teamBlue = scoreboard.getTeam("druzynaniebieska");
				Team teamGreen = scoreboard.getTeam("druzynazielona");
				Team teamYellow = scoreboard.getTeam("druzynazolta");
				if (teamRed == null) {
					sender.sendMessage("Błąd! Niezarejestrowana drużyna czerwona...");
					return true;
				}
				if (teamBlue == null) {
					sender.sendMessage("Błąd! Niezarejestrowana drużyna niebieska...");
					return true;
				}
				if (teamGreen == null) {
					sender.sendMessage("Błąd! Niezarejestrowana drużyna zielona...");
					return true;
				}
				if (teamYellow == null) {
					sender.sendMessage("Błąd! Niezarejestrowana drużyna żólta...");
					return true;
				}
				Team selectedTeam;
				 */
				int team = 0;
				String commandEI;

				List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
				Collections.shuffle(playerList);

				for (Player players : playerList) {
					if (isPlayerInEvent(players, sender)) {
						if (team == 0) {
							//selectedTeam = teamRed;
							commandEI = "ei giveslot " + players.getName() + " druzyna_czerwona 1 38 true";
						} else if (team == 1) {
							//selectedTeam = teamBlue;
							commandEI = "ei giveslot " + players.getName() + " druzyna_niebieska 1 38 true";
						} else if (team == 2) {
							//selectedTeam = teamYellow;
							commandEI = "ei giveslot " + players.getName() + " druzyna_zolta 1 38 true";
						} else {
							//selectedTeam = teamGreen;
							commandEI = "ei giveslot " + players.getName() + " druzyna_zielona 1 38 true";
						}
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandEI);
						//selectedTeam.addEntry(players.getName());
						team++;
						if (team == countTeams) team = 0;
					}
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lDrużyny zostały przydzielone."));
			}
			case "ann" -> {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					PlayerData pd = PlayerData.get(player);
					if (pd.eventAnnChat) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Wiadomości na środku ekranu: &c&lwyłączone&7."));
						pd.eventAnnChat = false;
					} else {
						if (isPlayerInEvent(player, null)) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Wiadomości na środku ekranu: &a&lwłączone&7."));
							pd.eventAnnChat = true;
						} else
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Nie jesteś na evencie."));

					}
				} else {
					sender.sendMessage("Komenda dostępna tylko z poziomu gry.");
				}
			}
		}
		return true;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			List<String> cmdlist = new ArrayList<>();
			cmdlist.addAll(Arrays.asList("randomplayer", "countplayers", "teamplayers", "ann"));
			return StringUtil.copyPartialMatches(args[0], cmdlist, new ArrayList<>());
		}
		return Collections.emptyList();
	}

	private boolean usage(CommandSender s) {
		s.sendMessage(Utils.color("&7Użyj: /evtools [randomplayer, countplayers, teamplayers, ann]"));
		return true;
	}

	// Czy gracz jest na evencie
	private boolean isPlayerInEvent(Player player, CommandSender ignore) {
		if (ignore != null) {
			if (ignore instanceof Player) {
				Player sender = (Player) ignore;
				if (sender == player)
					return false;// false
			}
		}


		if (player.getWorld().getName().equalsIgnoreCase("world_event"))// world_event
			return true;

		return false;
	}
}
