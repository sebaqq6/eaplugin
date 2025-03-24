package pl.eadventure.plugin.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.RollTool;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Command_evtools implements TabExecutor {
	boolean rollingPlayers = false;
	boolean kickingFromEvent = false;
	boolean rollOut = false;

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
						startRollingPlayerEffect(player);
					}
				}
				startRollingPlayerEffect(sender);

				if (playersInEventWorld.isEmpty()) {
					sender.sendMessage(Utils.color("&7Brak graczy na evencie!"));
				} else {
					rollingPlayers = true;
					Random random = new Random();
					int rollCount = 10;
					long interval = 10L;

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
								lastGlowingPlayer.setGlowing(false);

								Player chosenPlayer = playersInEventWorld.get(random.nextInt(playersInEventWorld.size()));
								chosenPlayer.setGlowing(true);

								new BukkitRunnable() {
									@Override
									public void run() {
										if (chosenPlayer.isOnline()) {
											chosenPlayer.setGlowing(false);
										}
									}
								}.runTaskLater(EternalAdventurePlugin.getInstance(), 200L);

								for (Player player : Bukkit.getOnlinePlayers()) {
									if (isPlayerInEvent(player, sender)) {
										endRollingPlayerEffect(player, chosenPlayer);
									}
								}
								endRollingPlayerEffect(sender, chosenPlayer);
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
					sender.sendMessage(Utils.color("&7Użyj: /evtools teamsplayer [ilość drużyn]"));
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
			case "kickall" -> {
				if (kickingFromEvent) {
					sender.sendMessage(Utils.color("&7Aktualnie trwa wyrzucanie graczy z mapy eventowej..."));
					return true;
				}

				if (args.length < 3) {
					Utils.commandUsageMessage(sender, "/evtools kickall [max_group(domyślnie: 1)] [tick_period(domyślnie: 21)]");
					return true;
				}
				Integer var_maxGroup = 1;
				Integer var_tickPeriod = 21;
				try {
					var_maxGroup = Integer.valueOf(args[1]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.color("&7Nieprawidłowa wartość max_group (wartość nie jest cyfrą)."));
					return true;
				}

				try {
					var_tickPeriod = Integer.valueOf(args[2]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.color("&7Nieprawidłowa wartość tick_period (wartość nie jest cyfrą)."));
					return true;
				}
				if (var_maxGroup < 1 || var_tickPeriod < 1) {
					sender.sendMessage(Utils.color("&7Nieprawidłowe parametry."));
					return true;
				}

				print.debug("var_maxGroup: " + var_maxGroup);
				print.debug("var_tickPeriod: " + var_tickPeriod);

				kickingFromEvent = true;
				World world = Bukkit.getWorld("world");
				Random random = new Random();
				Integer finalVar_maxGroup = var_maxGroup;
				new BukkitRunnable() {
					@Override
					public void run() {
						int kickedPlayers = 0;
						for (Player player : Bukkit.getOnlinePlayers()) {
							if (!isPlayerInEvent(player, sender)) continue;
							double randomDouble = 1 + (3 * random.nextDouble());
							Location tpPos = new Location(world, 31 + randomDouble, 169, -22 - randomDouble);
							new BukkitRunnable() {
								@Override
								public void run() {
									player.teleport(tpPos);
								}
							}.runTask(EternalAdventurePlugin.getInstance());
							kickedPlayers++;
							if (kickedPlayers >= finalVar_maxGroup) break;
						}
						if (kickedPlayers == 0) {
							sender.sendMessage(Utils.mm("<#FF0000>Procedura wyrzucania graczy z mapy eventowej <bold>zakończona</bold>."));
							kickingFromEvent = false;
							cancel();
						}
					}
				}.runTaskTimerAsynchronously(EternalAdventurePlugin.getInstance(), 20L, var_tickPeriod);
			}
			case "rollout" -> {
				if (rollOut) {
					sender.sendMessage(Utils.color("&7Aktualnie trwa rollout..."));
					return true;
				}
				if (args.length < 2) {
					Utils.commandUsageMessage(sender, "/evtools rollout [czas w sekundach]");
					return true;
				}
				Integer time = 20;
				try {
					time = Integer.valueOf(args[1]);
				} catch (NumberFormatException e) {
					sender.sendMessage(Utils.color("&7Nieprawidłowa wartość czasu(wartość nie jest cyfrą)."));
					return true;
				}
				if (sender instanceof Player player) {
					AtomicReference<Component> message = new AtomicReference<>(Utils.mm(
							"<dark_purple><bold>༺</bold></dark_purple><dark_purple><strikethrough>-----</strikethrough>"
									+ "<dark_purple><bold>༻</bold></dark_purple> <green><bold>START /ROLL</bold></green> "
									+ "<dark_purple><bold>༺</bold></dark_purple><dark_purple><strikethrough>-----</strikethrough>"
									+ "<dark_purple><bold>༻</bold></dark_purple>"
					));
					AtomicReference<Location> playerLocation = new AtomicReference<>(player.getLocation());
					for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
						if (nearbyPlayer.getLocation().distance(playerLocation.get()) <= 50) {
							nearbyPlayer.sendMessage(message.get());
						}
					}

					rollOut = true;
					PlayerData pd = PlayerData.get(player);
					RollTool rt = pd.rollTool;
					if (rt == null) {
						rt = new RollTool();
						pd.rollTool = rt;

					}
					//start and get roll result
					AtomicReference<String> playersWin = new AtomicReference<>("-brak-");
					AtomicInteger bestRoll = new AtomicInteger(0);
					rt.startRegisterRolls(time, result -> {
						List<String> winners = new ArrayList<>();

						for (Map.Entry<String, Integer> entry : result.entrySet()) {
							winners.add(entry.getKey());
							bestRoll.set(entry.getValue());
						}

						if (!winners.isEmpty()) {
							playersWin.set(String.join(", ", winners));
						}
						//send message
						message.set(Utils.mm(
								"<dark_purple><bold>༺</bold></dark_purple><dark_purple><strikethrough>-----</strikethrough>"
										+ "<dark_purple><bold>༻</bold></dark_purple> <yellow><bold>WYGRYWA:</bold></yellow> "
										+ "<green>" + playersWin + "</green> <dark_purple>-</dark_purple> <yellow>" + bestRoll + "</yellow> "
										+ "<dark_purple><bold>༺</bold></dark_purple><dark_purple><strikethrough>-----</strikethrough>"
										+ "<dark_purple><bold>༻</bold></dark_purple>"
						));
						playerLocation.set(player.getLocation());
						for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
							if (nearbyPlayer.getLocation().distance(playerLocation.get()) <= 50) {
								nearbyPlayer.sendMessage(message.get());
							}
						}
						rollOut = false;
					});
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
			cmdlist.addAll(Arrays.asList("randomplayer", "countplayers", "teamplayers", "ann", "kickall", "rollout"));
			return StringUtil.copyPartialMatches(args[0], cmdlist, new ArrayList<>());
		}
		return Collections.emptyList();
	}

	private boolean usage(CommandSender s) {
		s.sendMessage(Utils.color("&7Użyj: /evtools [randomplayer, countplayers, teamplayers, ann, kickall, rollout]"));
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

	//rolling effect
	private void startRollingPlayerEffect(CommandSender sender) {
		//sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eRozpoczęto losowanie..."));

		if (sender instanceof Player player) {
			player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&f&l\uD83C\uDFB2"),
					ChatColor.translateAlternateColorCodes('&', "&e&kxxxxxxxx"), 10, 140, 10);
			player.playSound(player.getLocation(), "my_sounds:sounds.clock.ticking",
					SoundCategory.MASTER, 1.0f, 2.0f);
		}
	}

	private void endRollingPlayerEffect(CommandSender sender, Player chosenPlayer) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&lWylosowany gracz: &a&l" + chosenPlayer.getName()));

		if (sender instanceof Player player) {
			player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&f&l\uD83C\uDFB2"),
					ChatColor.translateAlternateColorCodes('&', "&a" + chosenPlayer.getName()), 10, 140, 10);
			player.stopSound("my_sounds:sounds.clock.ticking");
			player.playSound(player.getLocation(), "my_sounds:sounds.treasury",
					SoundCategory.MASTER, 1.0f, 0.8f);
		}
	}

}
