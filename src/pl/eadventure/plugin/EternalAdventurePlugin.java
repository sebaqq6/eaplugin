package pl.eadventure.plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.garbagemule.MobArena.MobArena;
import ct.ajneb97.api.ComplexTurretsAPI;
import fr.skytasul.glowingentities.GlowingBlocks;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.API.ComplexTurretValidation;
import pl.eadventure.plugin.API.Placeholders;
import pl.eadventure.plugin.API.ProtocolLibAPI;
import pl.eadventure.plugin.Commands.*;
import pl.eadventure.plugin.Events.*;
import pl.eadventure.plugin.FunEvents.FunEventsManager;
import pl.eadventure.plugin.Modules.*;
import pl.eadventure.plugin.Modules.Top.TopBreakBlocks;
import pl.eadventure.plugin.Modules.Top.TopDonate;
import pl.eadventure.plugin.Modules.Top.TopGearScore;
import pl.eadventure.plugin.Modules.Top.TopTimePlayerPlayed;
import pl.eadventure.plugin.Utils.*;

import java.net.InetAddress;
import java.util.HashMap;

/*NOTATKI:
 * ALT+J - zannaczenie tego samego
 * CTRL+ALT+SHIFT - tworzenie kursorów
 *
 */

public final class EternalAdventurePlugin extends JavaPlugin {
	private static EternalAdventurePlugin instance;// Main instance
	private static Economy vault = null;// economy vault instance
	private static MySQLStorage storage;
	private static FileConfiguration config;
	private static Placeholders placeholders;
	private static LuckPerms luckPerms;
	private static ProtocolManager protocolManager;
	private static GlowingBlocks glowingBlocksAPI;
	private static MobArena mobarena;
	private static playerPrivateChatEvent privateChatEvent;
	private static ProtocolLibAPI plAPI;


	// public static int publicznaZmienna = 0;
	@Override
	public void onEnable() {
		instance = this;
		// Config
		config = getConfig();
		config.addDefault("debug", false);
		config.addDefault("leavesDecaySystem", false);
		config.addDefault("mysql.host", "127.0.0.1");
		config.addDefault("mysql.port", 3306);
		config.addDefault("mysql.database", "database");
		config.addDefault("mysql.user", "user");
		config.addDefault("mysql.password", "password");
		config.options().copyDefaults(true);
		saveConfig();
		// load config
		print.setDebug(config.getBoolean("debug"));
		LeavesDecay.active(config.getBoolean("leavesDecaySystem"));
		//Uruchomienie API Glowning
		//glowingBlocksAPI = new GlowingBlocks(this);
		// Wpięcie do VaultApi
		if (!setupEconomy()) {
			print.error("Nie wykryto pluginu ekonomii dla VaultAPI!");
			return;
		}
		//Wpięcie do PlaceolderAPI
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
			placeholders = new Placeholders(this);
			placeholders.register();
		} else print.error("Nie wykryto PlaceholderAPI!");
		//Wpięcie LuckPerms
		luckPerms = LuckPermsProvider.get();
		//Wpięcie ProtocoLib
		protocolManager = ProtocolLibrary.getProtocolManager();
		//Wpięie mobarena
		setupMobArena();
		// Anty LiquidBounce Completion crash... - MOVE THIS TO ANOTHER CLASS...
		protocolManager.addPacketListener(new PacketAdapter(
				this,
				ListenerPriority.NORMAL,
				PacketType.Play.Client.TAB_COMPLETE
		) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				PacketContainer packet = e.getPacket();
				String message = packet.getStrings().read(0);
				if (message.length() > 256) {
					Player player = e.getPlayer();
					print.info("Blokuje atak (Tab)Completion od gracza: " + player.getName());
					e.setCancelled(true);
					Bukkit.getScheduler().runTask(EternalAdventurePlugin.getInstance(), () -> {// Run on the next minecraft
						player.kickPlayer("Kicked for spamming");
					});
				}

			}
		});
		protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Status.Server.SERVER_INFO) {
			final HashMap<InetAddress, Long> lastPingInfo = new HashMap<>();

			@Override
			public void onPacketSending(PacketEvent event) {
				//InetAddress ip = event.getPlayer().getAddress().getAddress();
				//print.debug("[MC-PING]: " + ip.getHostAddress());
				/*if(ip == null || !ip.getHostAddress().equalsIgnoreCase("127.0.0.1")) {
					print.error(String.format("IP %s odpytało serwer poza TCPShield! Odpowiedź została anulowana.", ip.getHostAddress()));
					event.setCancelled(true);
					return;
				}*/
				/*long now = System.currentTimeMillis();
				long lastPingTime = 0;
				if(lastPingInfo.get(ip) != null)
				{
					lastPingTime = lastPingInfo.get(ip);
				}


				if (lastPingTime == 0 || now - lastPingTime >= 10000) {
					// Aktualizujemy czas ostatniego pingu
					lastPingInfo.put(ip, now);
					print.debug("[MC-PING-OK]: "+ip.getHostAddress());
					//updateLastPingTime(event.getPlayer().getUniqueId(), now); // Metoda do aktualizacji czasu ostatniego pingu dla gracza
				} else {
					print.debug("[MC-PING-DROP]: "+ip.getHostAddress());
					// Anulujemy wysłanie pakietu
					event.setCancelled(true);
				}*/

			}
		});
		// EVENTY
		getServer().getPluginManager().registerEvents(new playerJoinEvent(), this);
		getServer().getPluginManager().registerEvents(new playerQuitEvent(), this);
		getServer().getPluginManager().registerEvents(new playerDeathEvent(), this);
		getServer().getPluginManager().registerEvents(new playerRespawnEvent(), this);
		getServer().getPluginManager().registerEvents(new playerDropEvent(), this);
		getServer().getPluginManager().registerEvents(new playerChatEvent(), this);
		getServer().getPluginManager().registerEvents(new playerCommandPreprocessEvent(), this);
		getServer().getPluginManager().registerEvents(new serverCommandEvent(), this);
		getServer().getPluginManager().registerEvents(new playerPreLoginEvent(), this);
		getServer().getPluginManager().registerEvents(new playerInteractEvent(), this);
		getServer().getPluginManager().registerEvents(new playerChangeArmorEvent(), this);
		getServer().getPluginManager().registerEvents(new playerPlaceTurret(), this);
		getServer().getPluginManager().registerEvents(new leavesDecayEvent(), this);
		getServer().getPluginManager().registerEvents(new playerPlaceBlock(), this);
		getServer().getPluginManager().registerEvents(new playerBlockBreakEvent(), this);
		getServer().getPluginManager().registerEvents(new playerPickupItemEvent(), this);
		getServer().getPluginManager().registerEvents(new playerInventoryOpen(), this);
		getServer().getPluginManager().registerEvents(new onProjectileLaunchEvent(), this);
		getServer().getPluginManager().registerEvents(new entityDamageEvent(), this);
		getServer().getPluginManager().registerEvents(new playerInteractEntityEvent(), this);

		//CHANNEL FOR VELCITY
		getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:relay");

		// KOMENDY
		this.getCommand("eap").setExecutor(new Command_eap());
		this.getCommand("a").setExecutor(new Command_a());
		this.getCommand("evtools").setExecutor(new Command_evtools());
		this.getCommand("rozsypanka").setExecutor(new Command_rozsypanka());
		this.getCommand("ban").setExecutor(new Command_ban());
		this.getCommand("unban").setExecutor(new Command_unban());
		this.getCommand("banlist").setExecutor(new Command_banlist());
		this.getCommand("kartoteka").setExecutor(new Command_kartoteka());
		this.getCommand("warn").setExecutor(new Command_warn());
		this.getCommand("warns").setExecutor(new Command_warns());
		this.getCommand("warnlist").setExecutor(new Command_warnlist());
		this.getCommand("kick").setExecutor(new Command_kick());
		this.getCommand("mute").setExecutor(new Command_mute());
		this.getCommand("unmute").setExecutor(new Command_unmute());
		this.getCommand("mutelist").setExecutor(new Command_mutelist());
		this.getCommand("dzialka").setExecutor(new Command_dzialka());
		this.getCommand("chunkhunt").setExecutor(new Command_chunkhunt());
		this.getCommand("viewlog").setExecutor(new Command_viewlog());
		this.getCommand("ogloszenia").setExecutor(new Command_ogloszenie());
		this.getCommand("excellentcratesproxy").setExecutor(new Command_excellentcratesproxy());
		this.getCommand("fe").setExecutor(new Command_fe());
		this.getCommand("vconsole").setExecutor(new Command_vconsole());
		this.getCommand("creative").setExecutor(new Command_creative());
		this.getCommand("adminrank").setExecutor(new Command_adminrank());
		this.getCommand("playerhiddentabname").setExecutor(new Command_playerhiddentabname());
		this.getCommand("extitle").setExecutor(new Command_extitle());
		this.getCommand("donating").setExecutor(new Command_donating());
		this.getCommand("aka").setExecutor(new Command_aka());
		this.getCommand("live").setExecutor(new Command_streamer());
		this.getCommand("fixresourcepack").setExecutor(new Command_fixresourcepack());
		this.getCommand("fixmob").setExecutor(new Command_fixmob());
		this.getCommand("redflag").setExecutor(new Command_redflag());
		this.getCommand("eqs").setExecutor(new Command_eqs());
		//
		ComplexTurretsAPI.registerApiTargetValidations(this, new ComplexTurretValidation());
		// MySQL
		gVar.mysqlHost = config.getString("mysql.host");
		gVar.mysqlPort = config.getInt("mysql.port");
		gVar.mysqlDatabase = config.getString("mysql.database");
		gVar.mysqlUser = config.getString("mysql.user");
		gVar.mysqlPassword = config.getString("mysql.password");
		new BukkitRunnable() {
			@Override
			public void run() {
				storage = new MySQLStorage(gVar.mysqlHost, gVar.mysqlPort, gVar.mysqlDatabase, gVar.mysqlUser, gVar.mysqlPassword);
				if (storage.isConnect()) {
					print.ok("Połączono z bazą danych MySQL!");
					TimersForAllPlayers.startTimers(getInstance());
					TopTimePlayerPlayed.load(storage, 20);
					PunishmentSystem.init(storage);
					ServerLogManager.enable(storage);
					gVar.topBreakBlocks = new TopBreakBlocks(storage, 20);
					gVar.announceManager = new AnnounceManager(instance, storage);
					gVar.topGearScore = new TopGearScore(storage, 20);
					gVar.topDonate = new TopDonate(storage, 10);
					gVar.liveStream = new LiveStream();
					gVar.eqSaver = new EqSaver(instance, storage);
					//gVar.onlineCountHistory = new OnlineCountHistory(storage, getInstance());
					storage.execute("TRUNCATE playersonline;");
					PlayerData.fixSessions();
				} else
					print.error("Błąd połączenia z MySQL!");
			}
		}.runTaskAsynchronously(this);
		//Other loads
		MagicGUI.tryToLoadFor(this);
		HomesInterface.tryLoad(this);
		gVar.customItems = Utils.loadItemsFromFiles();
		Command_rozsypanka.autoInit();
		leavesDecayEvent.initCleanupBuffer();
		RegionCommandLooper.load(this);
		GearScoreCalculator.loadConfig();
		ArrowFix.run(this);
		MobFixer.load();

		//CrackComplexTurret.runBypassForCracked(this);
		gVar.colorIssueResolverIA = new ColorIssueResolverIA();
		gVar.colorIssueResolverIA.loadDataFromConfig();
		gVar.funEventsManager = new FunEventsManager(this);
		gVar.chatInputCapture = new ChatInputCapture(this);
		//
		privateChatEvent = new playerPrivateChatEvent();
		plAPI = new ProtocolLibAPI(protocolManager, this);
		///
		// SCHEDULES
		/*
		 * new BukkitRunnable() { public void run() {
		 * print.debug("Wątek asynchroniczny!"); storage.
		 * execute("INSERT INTO `dbtest`(`nick`, `uuid`, `kasa`) VALUES ('test','jakisuuid','2.0');"
		 * ); cancel(); } }.runTaskTimerAsynchronously(this, 20L, 20L);
		 */
		//Eternal Adventure Graffiti
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + " _____ _                        _      _       _                 _                  \n");
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "| ____| |_ ___ _ __ _ __   __ _| |    / \\   __| |_   _____ _ __ | |_ _   _ _ __ ___ \n");
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "|  _| | __/ _ \\ '__| '_ \\ / _` | |   / _ \\ / _` \\ \\ / / _ \\ '_ \\| __| | | | '__/ _ \\\n");
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "| |___| ||  __/ |  | | | | (_| | |  / ___ \\ (_| |\\ V /  __/ | | | |_| |_| | | |  __/\n");
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "|_____|\\__\\___|_|  |_| |_|\\__,_|_| /_/   \\_\\__,_| \\_/ \\___|_| |_|\\__|\\__,_|_|  \\___|\n");
		print.info("[EternalAdventurePlugin] Wczytywanie pluginu zakończone!");
	}

	@Override
	public void onDisable() {
		PlayerData.fixSessions();
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
			placeholders.unregister();
		}
		MagicGUI.tryToUnload();
		HomesInterface.tryUnload();
		ServerLogManager.disable();
		//glowingBlocksAPI.disable();
		print.info("[EternalAdventurePlugin] Plugin został wyłączony!");
	}

	public static EternalAdventurePlugin getInstance() {
		return instance;
	}

	public static FileConfiguration getMainConfig() {
		return config;
	}

	public static GlowingBlocks getGlowningBlockAPI() {
		return glowingBlocksAPI;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		vault = rsp.getProvider();
		return vault != null;
	}

	public static Economy getEconomy() {
		return vault;
	}

	public static LuckPerms getLuckPerms() {
		return luckPerms;
	}

	public static MySQLStorage getMySQL() {
		return storage;
	}

	public static MobArena getMobArena() {
		return mobarena;
	}

	private void setupMobArena() {
		Plugin plugin = getServer().getPluginManager().getPlugin("MobArena");
		if (plugin == null) {
			print.error("Nie udało się podłączyć do MobArena API.");
			return;
		}
		mobarena = (MobArena) plugin;
		print.ok("Udało się wpiąć do MobArena API.");
	}

	public static playerPrivateChatEvent getPrivateChatEvent() {
		return privateChatEvent;
	}

	public static PlaceholderAPIPlugin getPlaceHolderAPI() {
		return placeholders.getPlaceholderAPI();
	}

	public static ProtocolManager getProtocolManager() {
		return protocolManager;
	}
}