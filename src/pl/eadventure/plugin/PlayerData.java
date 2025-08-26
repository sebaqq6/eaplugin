package pl.eadventure.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Modules.Chat.Channel;
import pl.eadventure.plugin.Modules.Chat.Chat;
import pl.eadventure.plugin.Modules.Chat.IgnoreList;
import pl.eadventure.plugin.Modules.HomesInterface;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Modules.RegionCommandLooper;
import pl.eadventure.plugin.Modules.RollTool;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerData {
	//head
	private Player player = null;
	private static HashMap<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
	//temp data
	public boolean eventAnnChat = false;//player in Annuance chat mode
	public boolean decayDebug = false;
	public boolean deathOnPVPArena = false;//player last death on arena pvp
	public ItemStack[] itemsWhileDeath = null;//items while death player
	public ItemStack[] armorWhileDeath = null;//armor while death
	public int itemsCountWhileDeath = 0;//count items while death
	public int armorCountWhileDeath = 0;//count armor while death
	public int sessionOnlineSeconds = 0;
	public PunishmentSystem.WarnData warnData = null;
	public HomesInterface homesInterface = null;
	public String clientBrand = "null";
	public RegionCommandLooper regionCommandLooper = null;
	public ItemStack[] itemsBackupCreative = null;
	public ItemStack[] armorBackupCreative = null;
	public Location creativeLastPlacedPos = null;
	public Location creativeLastBreakPos = null;
	public boolean creativeMode = false;
	public String gearScore = "0";
	public String gsRaw = "0";
	public Timestamp lastSpec = null;
	public RollTool rollTool = null;
	public boolean freeze = false;
	public Timestamp lastTeleport = null;
	public String replayMsgNick = null;
	public Channel chatChannel = null;
	public List<Channel> joinedChatChannels = new ArrayList<>();
	//MySQL data
	String nick;
	public int dbid = 0;//database ID
	private int sessionId = 0;
	public int registerDate = 0;
	public boolean immunity = false; //
	public int onlineHours = 0, onlineMinutes = 0, onlineSeconds = 0;
	public int maxSessionOnlineSeconds = 0;
	public int mutedExpire = 0;
	public String mutedBy = " ";
	public String mutedReason = " ";
	public int breakBlocksCount = 0;
	public int afkTime = 0;
	public int unParticipateLive = 0;
	public boolean disabledMsg = false;
	public boolean enabledSpy = false;
	public boolean enabledRangedSpy = false;
	public IgnoreList ignoreList = null;
	//stream live
	public int isStreamer = 0;
	public String streamerService = " ";
	public String streamerURL = " ";
	public boolean onLiveStream = false;


	//end variables
	public PlayerData(Player player) {
		if (player == null) return;
		this.player = player;
		this.lastTeleport = Timestamp.from(Instant.now());
		players.put(player.getUniqueId(), this);
		if (player.getAddress().getAddress().getHostAddress() == null) return;
		print.debug("Gracz: " + player.getName() + " - stworzono instancje danych!");
		//join chat channels delay
		new BukkitRunnable() {
			@Override
			public void run() {
				Chat.autoJoinChannels(player);
			}
		}.runTaskLater(EternalAdventurePlugin.getInstance(), 50L);
		//load data from mysql
		loadDataFromMySQL(player);
	}

	public static PlayerData get(Player player) {
		if (players.containsKey(player.getUniqueId())) return players.get(player.getUniqueId());
		else return new PlayerData(player);
	}

	public static void free(Player player) {
		if (players.containsKey(player.getUniqueId())) {
			players.remove(player.getUniqueId());
		} else print.error("Gracz: " + player.getName() + " - Brak instancji danych! Coś jest nie tak!");
	}

	public void loadDataFromMySQL(Player player) {
		regionCommandLooper = new RegionCommandLooper(player);
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		if (storage.isConnect()) {
			//load WarnData
			warnData = new PunishmentSystem.WarnData();
			warnData.load(player.getUniqueId());
			//load warn data end
			UUID uuid = player.getUniqueId();
			String sql = "SELECT * FROM `players` WHERE `uuid`='" + uuid + "';";

			storage.query(sql, queryResult -> {
				if (queryResult != null) {
					int numRows = (int) queryResult.get("num_rows");
					//ArrayList<HashMap<Object, Object>> rows = (ArrayList<HashMap<Object, Object>>) queryResult.get("rows");
					if (numRows >= 1) {
						handleExistingPlayer(queryResult);
					} else {
						registerNewPlayer(storage);
					}
				} else {
					print.error("PlayerData->loadDataFromMySQL - błąd zapytania:");
					print.error(sql);
				}
			});
		} else {
			print.error("PlayerData->loadDataFromMySQL - nie udało się ustanowić połączenia!");
		}
	}

	//Player Exist - Load data...
	private void handleExistingPlayer(HashMap<Object, Object> queryResult) {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");

		dbid = (int) row.get("id");
		sessionId = 0;
		print.debug("Gracz: " + player.getName() + " - posiada konto EAP(ID: " + dbid + ").");
		//Load data start
		nick = (String) row.get("nick");
		registerDate = (int) row.get("registerdate");
		immunity = ((int) row.get("immunity") == 1);
		onlineHours = (int) row.get("onlineHours");
		onlineMinutes = (int) row.get("onlineMinutes");
		onlineSeconds = (int) row.get("onlineSeconds");
		maxSessionOnlineSeconds = (int) row.get("maxSessionOnlineSeconds");
		mutedExpire = (int) row.get("mutedExpire");
		mutedBy = (String) row.get("mutedBy");
		mutedReason = (String) row.get("mutedReason");
		breakBlocksCount = (int) row.get("breakBlocks");
		isStreamer = (int) row.get("streamer");
		streamerService = (String) row.get("streamer_service");
		streamerURL = (String) row.get("streamer_url");
		unParticipateLive = (int) row.get("unParticipateLive");
		disabledMsg = (int) row.get("disabledMsg") == 1;
		//load spy config - start
		if (player.hasPermission("eadventureplugin.cmd.spy")) {
			enabledSpy = (int) row.get("enabledSpy") == 1;
		}
		if (player.hasPermission("eadventureplugin.cmd.rangedspy")) {
			enabledRangedSpy = (int) row.get("enabledRangedSpy") == 1;
		}
		//load spy config - end


		//Load data end
		startSession();
		//Update some information
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(player.getAddress().getAddress().getHostAddress());
		parameters.add(player.getName());
		parameters.add(dbid);
		if (!nick.equalsIgnoreCase(player.getName())) {//player change nick detect
			print.info(String.format("Wykryto zmianę nicku %s -> %s", nick, player.getName()));
			PunishmentSystem.reloadFastCacheBanList();
			nick = player.getName();
		}
		storage.executeSafe("UPDATE players SET ip=?, nick=? WHERE id=?;", parameters);
	}

	//Register
	private void registerNewPlayer(MySQLStorage storage) {
		print.debug("Gracz: " + player.getName() + " - nie posiada konta EAP...");

		//Add to punish system
		PunishmentSystem.getListPlayersCanBeBanned().add(player.getName());

		PunishmentSystem.getListPlayersAll().add(player.getName());

		new BukkitRunnable() {
			@Override
			public void run() {
				//dbid = storage.executeGetInsertID("INSERT INTO `players`(`nick`, `uuid`, `registerdate`, `ip`) VALUES ('" + player.getName() + "', '" + uuid + "' , '" + Utils.getUnixTimestamp() + "', '" + player.getAddress().getAddress() + "');");
				ArrayList<Object> parameters = new ArrayList<>();
				parameters.add(player.getName());
				parameters.add(player.getUniqueId().toString());
				parameters.add(Utils.getUnixTimestamp());
				parameters.add(player.getAddress().getAddress().getHostAddress());
				dbid = storage.executeGetInsertID("INSERT INTO `players` (`nick`, `uuid`, `registerdate`, `ip`) VALUES (?, ?, ?, ?);", parameters);
				print.debug("Gracz: " + player.getName() + " - zarejestrowano konto EAP o ID: " + dbid + ".");
				sessionId = 0;
				startSession();
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	public void startSession() {
		//load ignore list
		ignoreList = new IgnoreList(dbid);
		if (dbid != 0 && sessionId == 0) {
			MySQLStorage storage = EternalAdventurePlugin.getMySQL();
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add(dbid);
			String sql = "INSERT INTO `sessions` (`uid`) VALUES (?);";
			sessionId = storage.executeGetInsertID(sql, parameters);
		}
	}

	public void updateSession() {
		if (sessionId != 0) {
			MySQLStorage storage = EternalAdventurePlugin.getMySQL();
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add(afkTime);
			parameters.add(sessionId);
			String sql = "UPDATE `sessions` SET `end`=CURRENT_TIMESTAMP, `afk`=? WHERE `id`=?";
			storage.executeSafe(sql, parameters);
		} else {
			startSession();
		}
	}

	public void resetSessionId() {
		sessionId = 0;
	}

	public static void fixSessions() {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
		if (storage.isConnect()) {
			String sql = "UPDATE `sessions` SET `end`=CURRENT_TIMESTAMP WHERE `end` IS NULL";
			storage.execute(sql);
		}

	}
}
