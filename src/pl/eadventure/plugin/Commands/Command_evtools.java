package pl.eadventure.plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.*;

public class Command_evtools implements TabExecutor {
	boolean rollingPlayers = false;

	@Override

	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0)
			return usage(sender);
		switch (args[0].toLowerCase()) {
			case "randomplayerold" -> {
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
			case "randomplayer" -> {
				if (rollingPlayers) {
					sender.sendMessage(Utils.color("&7Aktualnie trwa losowanie."));
					return true;
				}
				ArrayList<Player> playersInEventWorld = new ArrayList<>();
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (isPlayerInEvent(player, sender)) {
						playersInEventWorld.add(player);
						player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&f&l\uD83C\uDFB2"), ChatColor.translateAlternateColorCodes('&', "&e&kxxxxxxxx"), 10,
								140, 10);
						player.playSound(
								player.getLocation(),                     // Lokalizacja odtworzenia dźwięku
								"my_sounds:sounds.clock.ticking",              // Ścieżka do dźwięku (namespace:sound)
								SoundCategory.MASTER,                    // Kategoria dźwięku
								1.0f,                                    // Głośność
								2.0f                                     // Ton (1.0 = standardowy ton)
						);
					}
				}

				if (playersInEventWorld.isEmpty()) {
					sender.sendMessage(Utils.color("&7Brak graczy na evencie!"));
				} else {
					rollingPlayers = true;
					Random random = new Random();
					int rollCount = 10; // Ile razy "zakręci" ruletka
					long interval = 10L; // Co ile ticków zmienia się podświetlenie (10L = 0.5 sekundy)

					new BukkitRunnable() {
						int index = 0;
						Player lastGlowingPlayer = null;

						@Override
						public void run() {
							if (lastGlowingPlayer != null) {
								lastGlowingPlayer.setGlowing(false);
							}

							Player currentPlayer = playersInEventWorld.get(index % playersInEventWorld.size());
							currentPlayer.setGlowing(true);
							lastGlowingPlayer = currentPlayer;
							index++;

							if (index >= rollCount) {
								rollingPlayers = false;
								this.cancel();
								lastGlowingPlayer.setGlowing(false); // Upewniamy się, że ostatni gracz z ruletki zostaje wyłączony

								Player chosenPlayer = playersInEventWorld.get(random.nextInt(playersInEventWorld.size()));
								chosenPlayer.setGlowing(true);

								// Opóźnione wyłączenie podświetlenia o 200 ticków (10 sekund)
								new BukkitRunnable() {
									@Override
									public void run() {
										if (chosenPlayer.isOnline()) {
											chosenPlayer.setGlowing(false);
										}
									}
								}.runTaskLater(EternalAdventurePlugin.getInstance(), 200L);

								// Ogłoszenie wylosowanego gracza
								for (Player player : Bukkit.getOnlinePlayers()) {
									if (isPlayerInEvent(player, sender)) {
										player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lWylosowany gracz: &a&l" + chosenPlayer.getName()));
										player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&f&l\uD83C\uDFB2"), ChatColor.translateAlternateColorCodes('&', "&a" + chosenPlayer.getName()), 10,
												140, 10);
										player.stopSound("my_sounds:sounds.clock.ticking");
										player.playSound(
												player.getLocation(),                     // Lokalizacja odtworzenia dźwięku
												"my_sounds:sounds.treasury",              // Ścieżka do dźwięku (namespace:sound)
												SoundCategory.MASTER,                    // Kategoria dźwięku
												1.0f,                                    // Głośność
												0.8f                                     // Ton (1.0 = standardowy ton)
										);
									}
								}
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lWylosowany gracz: &a&l" + chosenPlayer.getName()));
							}
						}
					}.runTaskTimer(EternalAdventurePlugin.getInstance(), 0L, interval);
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
				//if (sender == player)
				//return false;// false
			}
		}


		if (player.getWorld().getName().equalsIgnoreCase("world_event"))// world_event
			return true;

		return false;
	}
}
