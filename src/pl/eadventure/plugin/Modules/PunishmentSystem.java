package pl.eadventure.plugin.Modules;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.*;
import pl.eadventure.plugin.gVar;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;


public class PunishmentSystem {
	private static MySQLStorage storage;
	private static final ArrayList<String> listPlayersCanBeBanned = new ArrayList<>();
	private static final ArrayList<String> listPlayersCanBeUnbanned = new ArrayList<>();
	private static final ArrayList<String> listPlayersAll = new ArrayList<>();

	public interface BanType {
		int NICK = 1;
		int NICK_UUID = 2;
		int NICK_UUID_IP = 3;
	}

	public record BanData(String nick, String ip, UUID uuid, String bannedByNick, int expiresTimestamp,
						  String bannedDate, String reason) {
		private static final HashMap<String, BanData> banForNick = new HashMap<>();
		private static final HashMap<String, BanData> banForIP = new HashMap<>();
		private static final HashMap<UUID, BanData> banForUUID = new HashMap<>();

		public BanData(String nick, String ip, UUID uuid, String bannedByNick, int expiresTimestamp, String bannedDate, String reason) {
			this.nick = nick;
			this.ip = ip;
			this.uuid = uuid;
			this.bannedByNick = bannedByNick;
			this.expiresTimestamp = expiresTimestamp;
			this.bannedDate = bannedDate;
			this.reason = reason;
			if (nick != null) banForNick.put(nick, this);
			if (ip != null) banForIP.put(ip, this);
			if (uuid != null) banForUUID.put(uuid, this);
		}

		public static BanData getByNick(String nick) {
			if (banForNick.containsKey(nick)) {
				BanData bd = banForNick.get(nick);
				if (bd.expiresTimestamp() < Utils.getUnixTimestamp() && bd.expiresTimestamp != -1) {
					banForNick.remove(nick);
					reloadFastCacheBanList();
					return null;
				}
				return banForNick.get(nick);
			}
			return null;
		}

		public static BanData getByIP(String ip) {
			if (banForIP.containsKey(ip)) {
				BanData bd = banForIP.get(ip);
				if (bd.expiresTimestamp() < Utils.getUnixTimestamp() && bd.expiresTimestamp != -1) {
					banForIP.remove(ip);
					reloadFastCacheBanList();
					return null;
				}
				return banForIP.get(ip);
			}
			return null;
		}

		public static BanData getByUUID(UUID uuid) {
			if (banForUUID.containsKey(uuid)) {
				BanData bd = banForUUID.get(uuid);
				if (bd.expiresTimestamp() < Utils.getUnixTimestamp() && bd.expiresTimestamp != -1) {
					banForUUID.remove(uuid);
					reloadFastCacheBanList();
					return null;
				}
				return banForUUID.get(uuid);
			}
			return null;
		}

		public static void clear() {
			banForNick.clear();
			banForIP.clear();
			banForUUID.clear();
		}


	}

	public static void init(MySQLStorage mysqlInstance) {
		storage = mysqlInstance;
		reloadBans();
		WarnData.deleteExpiredWarnsFromDB();
		new BukkitRunnable() {
			@Override
			public void run() {
				reloadFastCacheBanList();
			}
		}.runTaskTimer(EternalAdventurePlugin.getInstance(), 20L * 60L * 30L, 20L * 60L * 30L);
	}

	public interface LogType {
		int MUTE = 0;
		int BAN = 1;
		int KICK = 2;
		int WARN = 3;
	}

	public static void notifyMessage(int type, String targetName, String adminName, String reason, long expire) {
		//silence notify
		boolean disableNotify = false;
		if (reason.contains("-s")) {
			disableNotify = true;
			reason = reason.replace("-s", "");
			reason = reason.trim();
		}
		if (disableNotify) {
			Component message = null;
			switch (type) {
				case LogType.MUTE -> {
					String timeFormat = PunishmentSystem.getFormatedExpiresShort((int) expire);
					message = Utils.mm(String.format("<grey><bold>[<yellow>CICHY MUTE<grey>]</bold> Admin: <yellow>%s</yellow>, Gracz: <yellow>%s</yellow>, Czas: <yellow>%s</yellow>, Powód: <yellow>%s</yellow>", adminName, targetName, timeFormat, reason));
				}
				case LogType.BAN -> {
					String timeFormat = "Permanentny";
					if (expire != -1) {
						long expireTimestamp = Utils.getUnixTimestamp() + expire * 60L;
						timeFormat = PunishmentSystem.getFormatedExpiresShort((int) expireTimestamp);
					}
					message = Utils.mm(String.format("<grey><bold>[<yellow>CICHY BAN<grey>]</bold> Admin: <yellow>%s</yellow>, Gracz: <yellow>%s</yellow>, Czas: <yellow>%s</yellow>, Powód: <yellow>%s</yellow>", adminName, targetName, timeFormat, reason));
				}
				case LogType.KICK -> {
					message = Utils.mm(String.format("<grey><bold>[<yellow>CICHY KICK<grey>]</bold>  Admin: <yellow>%s</yellow>, Gracz: <yellow>%s</yellow>, Powód: <yellow>%s</yellow>", adminName, targetName, reason));
				}
				case LogType.WARN -> {
					String timeFormat = PunishmentSystem.getFormatedExpiresShort((int) expire);
					message = Utils.mm(String.format("<grey><bold>[<yellow>CICHY WARN<grey>]</bold> Admin: <yellow>%s</yellow>, Gracz: <yellow>%s</yellow>, Czas: <yellow>%s</yellow>, Powód: <yellow>%s</yellow>", adminName, targetName, timeFormat, reason));
				}
			}
			if (message != null) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (PlayerUtils.hasAnyAdminPermission(p)) {
						if (PlayerData.get(p).onLiveStream) continue;
						p.sendMessage(message);
					}
				}
			}
			return;
		}
		switch (type) {
			case LogType.MUTE -> {
				PlayerUtils.sendColorMessageToAll(String.format("&4&l&c&l%s &4&lzostał/a uciszony/a przez &3&l%s", targetName, adminName));
			}
			case LogType.BAN -> {
				PlayerUtils.sendColorMessageToAll(String.format("&4&l&c&l%s &4&lzostał/a zbanowany/a przez &3&l%s", targetName, adminName));
			}
			case LogType.KICK -> {
				PlayerUtils.sendColorMessageToAll(String.format("&4&l&c&l%s &4&lzostał/a wyrzucony/a przez &3&l%s", targetName, adminName));
			}
			case LogType.WARN -> {
				PlayerUtils.sendColorMessageToAll(String.format("&4&l&c&l%s &4&lotrzymał/a ostrzeżenie od &3&l%s", targetName, adminName));
			}
		}

		PlayerUtils.sendColorMessageToAll(String.format("&4&lPowód: &7%s", reason));

		//time format
		if (type != LogType.KICK) { //all others have time
			String timeFormat = "Permanentny";
			if (type == LogType.BAN) {//only ban have other time format...
				if (expire != -1) {
					long expireTimestamp = Utils.getUnixTimestamp() + expire * 60L;
					timeFormat = PunishmentSystem.getFormatedExpiresShort((int) expireTimestamp);
				}
			} else {
				timeFormat = PunishmentSystem.getFormatedExpiresShort((int) expire);
			}
			PlayerUtils.sendColorMessageToAll(String.format("&4&lCzas: &7%s", timeFormat));
		}
	}

	public static void log(int type, String nick, String adminNick, String note) {
		ArrayList<Object> parameteres = new ArrayList<>();
		parameteres.add(nick);
		parameteres.add(adminNick);
		parameteres.add(note);
		parameteres.add(type);
		storage.executeSafe("INSERT INTO logadmin (nick, adminNick, note, type) VALUES (?, ?, ?, ?);", parameteres);
	}

	public static void reloadBans() {
		storage.execute(String.format("UPDATE players SET banned=0, bannedBy=NULL, banExpires=NULL, banReason=NULL, banDate=NULL WHERE banExpires < %d AND banExpires!=-1;", Utils.getUnixTimestamp()));
		storage.query("SELECT nick, uuid, ip, banned, bannedBy, banExpires, banReason, banDate FROM players WHERE banned>=1 ORDER BY banDate DESC;", queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			BanData.clear();
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					int banType = (int) rows.get(i).get("banned");

					//timestamp to string
					Timestamp dateTimestamp = (Timestamp) rows.get(i).get("banDate");
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String date = sdf.format(dateTimestamp);

					if (banType == BanType.NICK) {
						new BanData((String) rows.get(i).get("nick"), null, null, (String) rows.get(i).get("bannedBy"), (int) rows.get(i).get("banExpires"), date, (String) rows.get(i).get("banReason"));
					} else if (banType == BanType.NICK_UUID) {
						String uuid = (String) rows.get(i).get("uuid");
						new BanData((String) rows.get(i).get("nick"), null, UUID.fromString(uuid), (String) rows.get(i).get("bannedBy"), (int) rows.get(i).get("banExpires"), date, (String) rows.get(i).get("banReason"));
					} else if (banType == BanType.NICK_UUID_IP) {
						String uuid = (String) rows.get(i).get("uuid");
						new BanData((String) rows.get(i).get("nick"), (String) rows.get(i).get("ip"), UUID.fromString(uuid), (String) rows.get(i).get("bannedBy"), (int) rows.get(i).get("banExpires"), date, (String) rows.get(i).get("banReason"));
					}
				}
			}
			print.info("Lista banów została przeładowana.");
		});
		reloadFastCacheBanList();
	}

	public static void reloadFastCacheBanList() {
		//All list clear
		listPlayersAll.clear();
		//Create list players can be banned
		storage.query("SELECT `nick` FROM players WHERE banned = 0;", queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			listPlayersCanBeBanned.clear();
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					String nick = (String) rows.get(i).get("nick");
					listPlayersCanBeBanned.add(nick);
					listPlayersAll.add(nick);
				}
			}
		});
		//Create list players can be unbanned
		storage.query("SELECT `nick` FROM players WHERE banned != 0 ORDER BY banDate DESC;", queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			listPlayersCanBeUnbanned.clear();
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					String nick = (String) rows.get(i).get("nick");
					listPlayersCanBeUnbanned.add(nick);
					listPlayersAll.add(nick);
				}
				//DELETE THIS! - generate random players
				/*for(int i = 0; i < 200; i++)
				{
					listPlayersCanBeUnbanned.add("Player"+i);
				}*/
			}
		});
	}

	public static BanData isBanned(String nick, String ip, UUID uuid) {
		BanData bd = BanData.getByNick(nick);
		if (bd == null) bd = BanData.getByIP(ip);
		if (bd == null) bd = BanData.getByUUID(uuid);
		return bd;//return ban data, or null if not is banned
	}

	public static void banPlayer(String playerNick, String adminNick, final int timeMinutes, int power, String reason) {
		ArrayList<Object> parametersSelect = new ArrayList<>();
		parametersSelect.add(playerNick);
		storage.querySafe("SELECT id FROM players WHERE nick=?", parametersSelect, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");
			if (numRows == 1) {
				ArrayList<Object> parametersExecute = new ArrayList<>();
				parametersExecute.add(power);
				parametersExecute.add(adminNick);
				long calculatedExpireTimestamp = -1;//default perm
				if (timeMinutes > 0) {
					calculatedExpireTimestamp = Utils.getUnixTimestamp() + (60L * timeMinutes);//if timeMinuter is higher than 0 calc timestamp
					//log
					int[] time = Utils.convertSecondsToTimeWithDays(60 * timeMinutes);
					log(LogType.BAN, playerNick, adminNick, String.format("%s. %dd, %dg, %dm, %ds.", reason, time[0], time[1], time[2], time[3]));
				} else {
					log(LogType.BAN, playerNick, adminNick, String.format("%s. Permanentny.", reason));
				}
				parametersExecute.add(calculatedExpireTimestamp);
				parametersExecute.add(reason);
				parametersExecute.add(Utils.getCurrentDate());
				parametersExecute.add(row.get("id"));
				storage.executeSafe("UPDATE players SET banned=?, bannedBy=?, banExpires=?, banReason=?, banDate=? WHERE id=?", parametersExecute);
				reloadBans();
				Player player = Bukkit.getPlayer(playerNick);
				if (player != null)
					player.kickPlayer(getBannedMessage(playerNick, adminNick, reason, (int) calculatedExpireTimestamp, Utils.getCurrentDate()));
			}
		});
	}

	public static void unbanPlayer(String playerNick) {
		ArrayList<Object> parametersSelect = new ArrayList<>();
		parametersSelect.add(playerNick);
		storage.querySafe("SELECT id FROM players WHERE nick=?", parametersSelect, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");
			if (numRows == 1) {
				ArrayList<Object> parametersExecute = new ArrayList<>();
				parametersExecute.add(row.get("id"));
				storage.executeSafe("UPDATE players SET banned=0, bannedBy=NULL, banExpires=NULL, banReason=NULL, banDate=NULL WHERE id=?", parametersExecute);
				reloadBans();
			}
		});
	}

	public static ArrayList<String> getListPlayersCanBeBanned() {
		return listPlayersCanBeBanned;
	}

	public static ArrayList<String> getListPlayersCanBeUnbanned() {
		return listPlayersCanBeUnbanned;
	}

	public static ArrayList<String> getListPlayersAll() {
		return listPlayersAll;
	}

	public static String getBannedMessage(String playerNick, String adminNick, String reason, int expiresTimestamp, String banDate) {
		reason = reason.replace("-s", "");//remove -s from kickMessage per player
		reason = reason.trim();
		String finalMessage = "&c&lJesteś zbanowany/a!\n" +
				"\n" +
				String.format("&cGracz: &3%s\n", playerNick) +
				String.format("&cNadał/a: &3%s\n", adminNick) +
				String.format("&cPowód: &7%s\n", reason) +
				String.format("&cNadano: &7%s\n", banDate) +
				String.format("&cPozostało: &7%s\n", getFormatedExpires(expiresTimestamp)) +
				"\n" +
				"&8Apeluj od kary na discordzie.\n" +
				"&5Discord: &5&ndiscord.eadventure.pl\n" +
				"&8Lub wykup unbana: &7&nsklep.eadventure.pl";
		return ChatColor.translateAlternateColorCodes('&', finalMessage);
	}

	public static String getFormatedExpires(int expiresTimestamp) {
		if (expiresTimestamp == -1) return "Permanentny";
		long remainingSeconds = expiresTimestamp - Utils.getUnixTimestamp();
		int[] remainingTime = Utils.convertSecondsToTimeWithDays((int) remainingSeconds);
		return String.format("%d dni, %d godz, %d min, %d sek", remainingTime[0], remainingTime[1], remainingTime[2], remainingTime[3]);
	}

	public static String getFormatedExpiresShort(int expiresTimestamp) {
		if (expiresTimestamp == -1) return "Permanentny";
		long remainingSeconds = expiresTimestamp - Utils.getUnixTimestamp();
		int[] remainingTime = Utils.convertSecondsToTimeWithDays((int) remainingSeconds);
		return String.format("%dd, %dg, %dm", remainingTime[0], remainingTime[1], remainingTime[2]);
	}

	public static void showBanListGUI(Player p, Integer page, boolean playerMode) {
		int playersPerPage = 45; // Limit of players per page
		int startIndex = (page - 1) * playersPerPage;
		int endIndex = Math.min(startIndex + playersPerPage, getListPlayersCanBeUnbanned().size());
		int totalPages = (int) Math.ceil((double) getListPlayersCanBeUnbanned().size() / playersPerPage);
		MagicGUI banListGUI = MagicGUI.create(Utils.color(String.format("&4&lLista banów (%d/%d)", page, Math.max(totalPages, 1))), 54);
		banListGUI.setAutoRemove(true);
		//print.debug(String.format("startIndex: %d, endIndex: %d", startIndex, endIndex));
		for (int i = startIndex; i < endIndex; i++) {
			if (i >= 0 && i < getListPlayersCanBeUnbanned().size()) {
				String playerBannedName = getListPlayersCanBeUnbanned().get(i);
				ArrayList<String> playerDescription = new ArrayList<>();
				BanData tempBanData = BanData.getByNick(playerBannedName);
				if (tempBanData == null) continue;
				if (!playerMode) {
					playerDescription.add(Utils.color(String.format("&r&7Powód: &3%s", tempBanData.reason())));
					playerDescription.add(Utils.color(String.format("&r&7Nadał/a: &3%s", tempBanData.bannedByNick())));
					playerDescription.add(Utils.color(String.format("&r&7Nadano: &3%s", tempBanData.bannedDate())));
					playerDescription.add(Utils.color(String.format("&r&7Pozostało: &3%s", getFormatedExpiresShort(tempBanData.expiresTimestamp()))));
					playerDescription.add(" ");
					playerDescription.add(Utils.color(String.format("&r&7Ban na nick: %s", (tempBanData.nick() != null) ? "&a✔" : "&c✘")));
					playerDescription.add(Utils.color(String.format("&r&7Ban na UUID: %s", (tempBanData.uuid() != null) ? "&a✔" : "&c✘")));
					playerDescription.add(Utils.color(String.format("&r&7Ban na IP: %s", (tempBanData.ip() != null) ? "&a✔" : "&c✘")));
				} else {
					playerDescription.add(Utils.color(String.format("&r&7Nadano: &3%s", tempBanData.bannedDate())));
					playerDescription.add(Utils.color(String.format("&r&7Pozostało: &3%s", getFormatedExpiresShort(tempBanData.expiresTimestamp()))));
				}
				if (p.hasPermission("eadventureplugin.cmd.unban") && !playerMode) {
					playerDescription.add(" ");
					playerDescription.add(Utils.color("&r&c&lSHIFT+PPM &7- aby odbanować."));
				}
				ItemStack playerHead = Utils.getPlayerHead(playerBannedName, Utils.color(String.format("&c&l%s", playerBannedName)), playerDescription);

				banListGUI.addItem(playerHead, (player, gui, slot, type) -> {
					if (ClickType.SHIFT_RIGHT == type) {
						if (player.hasPermission("eadventureplugin.cmd.unban") && !playerMode) {
							player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Gracz &2%s &7został &2pomyślnie &7odbanowany.", playerBannedName)));
							unbanPlayer(playerBannedName);
							banListGUI.setItem(slot, new ItemStack(Material.AIR));
						}
					}
				});
			}
		}

		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					banListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						showBanListGUI(p, page - 1, playerMode);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					banListGUI.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
				banListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					banListGUI.close(p);
				});
			} else if (x == 51) {
				if (endIndex < getListPlayersCanBeUnbanned().size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					banListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						showBanListGUI(p, nextPage, playerMode);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					banListGUI.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				banListGUI.setItem(x, navButton);
			}
		}
		banListGUI.open(p);
	}

	public record AdminLogEntry(String nick, String adminNick, String note, String date, int type) {
	}

	public static void showAdminLogGUI(Player p, String targetName, int entryType, Integer page, ArrayList<AdminLogEntry> adminLogList) {
		if (adminLogList == null) {
			ArrayList<AdminLogEntry> adminLog = new ArrayList<>();
			//load data
			String sql;
			ArrayList<Object> sqlParameters = new ArrayList<>();
			if (entryType == -1) {
				if (targetName == null) sql = "SELECT * FROM logadmin ORDER BY id DESC;";
				else {
					sql = "SELECT * FROM logadmin WHERE nick=? ORDER BY id DESC;";
					sqlParameters.add(targetName);
				}
			} else {
				if (targetName == null) {
					sql = "SELECT * FROM logadmin WHERE type=? ORDER BY id DESC;";
					sqlParameters.add(entryType);
				} else {
					sql = "SELECT * FROM logadmin WHERE nick=? AND type=? ORDER BY id DESC;";
					sqlParameters.add(targetName);
					sqlParameters.add(entryType);
				}
			}
			storage.querySafe(sql, sqlParameters, queryResult -> {
				int numRows = (int) queryResult.get("num_rows");
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
				if (numRows > 0) {
					for (int i = 0; i < numRows; i++) {
						String nick = (String) rows.get(i).get("nick");
						String adminNick = (String) rows.get(i).get("adminNick");
						String note = (String) rows.get(i).get("note");

						Timestamp dateTimestamp = (Timestamp) rows.get(i).get("date");
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String date = sdf.format(dateTimestamp);

						int entryTypeFromDB = (int) rows.get(i).get("type");
						adminLog.add(new AdminLogEntry(nick, adminNick, note, date, entryTypeFromDB));
					}
				}
				showAdminLogGUI(p, targetName, entryType, page, adminLog);
			});
		}
		if (adminLogList == null) return;
		//page variables
		int itemsPerPage = 45;
		int startIndex = (page - 1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, adminLogList.size());
		int totalPages = (int) Math.ceil((double) adminLogList.size() / itemsPerPage);
		String targetNameFinal = targetName;
		if (targetName == null) targetNameFinal = "-";
		String titleName = String.format("&4&lKartoteka %s (%d/%d)", targetNameFinal, page, Math.max(totalPages, 1));
		switch (entryType) {
			case LogType.MUTE ->
					titleName = String.format("&4&lMute %s (%d/%d)", targetNameFinal, page, Math.max(totalPages, 1));
			case LogType.BAN ->
					titleName = String.format("&4&lBany %s (%d/%d)", targetNameFinal, page, Math.max(totalPages, 1));
			case LogType.KICK ->
					titleName = String.format("&4&lKicki %s (%d/%d)", targetNameFinal, page, Math.max(totalPages, 1));
			case LogType.WARN ->
					titleName = String.format("&4&lWarny %s (%d/%d)", targetNameFinal, page, Math.max(totalPages, 1));
		}
		MagicGUI adminLogGUI = MagicGUI.create(Utils.color(titleName), 54);
		adminLogGUI.setAutoRemove(true);
		for (int i = startIndex; i < endIndex; i++) {
			if (i >= 0 && i < adminLogList.size()) {
				AdminLogEntry entry = adminLogList.get(i);
				if (entryType != -1 && entry.type() != entryType) continue;
				ArrayList<String> playerDescription = new ArrayList<>();
				String entryTypeName = "Niezidentyfikowany";
				playerDescription.add(Utils.color(String.format("&r&7Gracz: &3%s", entry.nick())));
				playerDescription.add(Utils.color(String.format("&r&7Admin: &3%s", entry.adminNick())));
				switch (entry.type()) {
					case LogType.MUTE -> entryTypeName = "MUTE";
					case LogType.BAN -> entryTypeName = "BAN";
					case LogType.KICK -> entryTypeName = "KICK";
					case LogType.WARN -> entryTypeName = "WARN";
				}
				playerDescription.add(Utils.color(String.format("&r&7Typ: &3%s", entryTypeName)));
				playerDescription.add(Utils.color(String.format("&r&7Data wpisu: &3%s", entry.date())));
				playerDescription.add(Utils.color("&r&7Szczegóły:"));
				playerDescription.add(Utils.color(String.format("&r&3%s", entry.note())));
				ItemStack playerHead = Utils.getPlayerHead(entry.nick(), Utils.color(String.format("&c&l%s", entry.nick())), playerDescription);
				adminLogGUI.addItem(playerHead);
			}
		}

		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if (x == 45) {
				ArrayList<String> description = new ArrayList<>();

				description.add(Utils.color(String.format("&r%s&7 BAN", (entryType == LogType.BAN || entryType == -1) ? "&a☑" : "&c☒")));
				description.add(Utils.color(String.format("&r%s&7 KICK", (entryType == LogType.KICK || entryType == -1) ? "&a☑" : "&c☒")));
				description.add(Utils.color(String.format("&r%s&7 WARN", (entryType == LogType.WARN || entryType == -1) ? "&a☑" : "&c☒")));
				description.add(Utils.color(String.format("&r%s&7 MUTE", (entryType == LogType.MUTE || entryType == -1) ? "&a☑" : "&c☒")));

				int newEntryType = entryType;
				switch (entryType) {
					case LogType.BAN -> newEntryType = LogType.KICK;
					case LogType.KICK -> newEntryType = LogType.WARN;
					case LogType.WARN -> newEntryType = LogType.MUTE;
					case LogType.MUTE -> newEntryType = -1;//-1 = all
					default -> newEntryType = LogType.BAN;
				}

				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hChange"), Utils.color("&r&7&lZmień typ"), description);
				int finalNewEntryType = newEntryType;
				adminLogGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					showAdminLogGUI(p, targetName, finalNewEntryType, 1, null);
				});
			} else if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					adminLogGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						showAdminLogGUI(p, targetName, entryType, page - 1, adminLogList);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					adminLogGUI.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
				adminLogGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					adminLogGUI.close(p);
				});
			} else if (x == 51) {
				if (endIndex < adminLogList.size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					adminLogGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						showAdminLogGUI(p, targetName, entryType, nextPage, adminLogList);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					adminLogGUI.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				adminLogGUI.setItem(x, navButton);
			}
		}
		adminLogGUI.open(p);
	}

	public record WarnEntry(int id, String nick, String adminNick, String reason, int expire, Timestamp date) {
		public String getFormatDate() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sdf.format(date);
		}

		public String getFormatExpire() {
			int expireSeconds = expire - (int) Utils.getUnixTimestamp();
			int[] timeExpire = Utils.convertSecondsToTimeWithDays(expireSeconds);
			return String.format("%dd, %dg, %dm, %ds", timeExpire[0], timeExpire[1], timeExpire[2], timeExpire[3]);
		}
	}

	public static class WarnData {
		public static final int maxWarns = 5;
		Vector<WarnEntry> warnList = new Vector<>();
		String playerName = null;
		boolean loaded = false;

		public void load(String playerNick) {
			playerName = playerNick;
			ArrayList<Object> sqlParams = new ArrayList<>();
			sqlParams.add(playerNick);
			sqlParams.add(Utils.getUnixTimestamp());
			String sql = "SELECT pw.*, p.nick " +
					"FROM playerwarns AS pw " +
					"JOIN players AS p ON pw.uuid = p.uuid " +
					"WHERE p.nick = ? AND pw.expire > ?;";
			storage.querySafe(sql, sqlParams, this::onDataLoaded);
		}

		public void load(UUID playerUUID) {
			playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
			if (playerName == null) {
				print.error(String.format("WarnLoad, błąd UUID: %s", playerUUID));
				return;
			}
			ArrayList<Object> sqlParams = new ArrayList<>();
			sqlParams.add(playerUUID.toString());
			sqlParams.add(Utils.getUnixTimestamp());
			String sql = "SELECT pw.*, p.nick " +
					"FROM playerwarns AS pw " +
					"JOIN players AS p ON pw.uuid = p.uuid " +
					"WHERE p.uuid = ? AND pw.expire > ?;";

			storage.querySafe(sql, sqlParams, this::onDataLoaded);
		}

		private void onDataLoaded(HashMap<Object, Object> queryResult) {
			warnList.clear();//clear acutal warns before load new
			if (queryResult == null) {
				loaded = true;
				return;
			}
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			int numRows = (int) queryResult.get("num_rows");
			if (numRows > 0) {
				for (int i = 0; i < numRows; i++) {
					int id = (int) rows.get(i).get("id");
					String nick = (String) rows.get(i).get("nick");
					String adminNick = (String) rows.get(i).get("adminNick");
					String reason = (String) rows.get(i).get("reason");
					int expire = (int) rows.get(i).get("expire");

					Timestamp dateTimestamp = (Timestamp) rows.get(i).get("date");

					warnList.add(i, new WarnEntry(id, nick, adminNick, reason, expire, dateTimestamp));
				}
			}
			loaded = true;
		}

		public static void deleteExpiredWarnsFromDB() {
			ArrayList<Object> sqlParams = new ArrayList<>();
			sqlParams.add(Utils.getUnixTimestamp());
			storage.executeSafe("DELETE FROM playerwarns WHERE expire<?", sqlParams);
		}

		public void deleteExpired() {
			List<WarnEntry> entriesToRemove = new ArrayList<>();
			for (WarnEntry entry : warnList) {
				if (entry.expire() <= Utils.getUnixTimestamp()) {
					entriesToRemove.add(entry);
				}
			}

			for (WarnEntry entryToRemove : entriesToRemove) {
				delete(entryToRemove);
			}
		}

		public void deleteAll() {
			List<WarnEntry> entriesToRemove = new ArrayList<>(warnList);

			for (WarnEntry entryToRemove : entriesToRemove) {
				delete(entryToRemove);
			}
		}

		public int size() {
			return warnList.size();
		}

		public boolean isLoaded() {
			return loaded;
		}

		public WarnEntry getEntry(int index) {
			return warnList.get(index);
		}

		public void add(String adminNick, String reason, int expire) {
			String sql = "INSERT INTO playerwarns (uuid, adminNick, reason, expire) " +
					"SELECT uuid, ?, ?, ? " +
					"FROM players " +
					"WHERE nick = ?;";

			ArrayList<Object> sqlParams = new ArrayList<>();
			sqlParams.add(adminNick);
			sqlParams.add(reason);
			sqlParams.add(expire);
			sqlParams.add(playerName);
			int id = storage.executeGetInsertID(sql, sqlParams);
			warnList.add(new WarnEntry(id, playerName, adminNick, reason, expire, new Timestamp(System.currentTimeMillis())));
			if (size() >= maxWarns) {
				deleteAll();//delete all warns
				int timeMinutes = 7 * 60 * 24;//days * minutes * hours = total minutes
				banPlayer(playerName, "CONSOLE", timeMinutes, BanType.NICK_UUID, "Przekroczona ilość warnów.");
				PlayerUtils.sendColorMessageToAll(String.format("&4&lGracz &c%s &4&lzostał zbanowany przez &3CONSOLE", playerName));
				PlayerUtils.sendColorMessageToAll("&4&lPowód: &7Przekroczona ilość warnów.");
			}
		}

		public void delete(WarnEntry warnEntryToRemove) {
			for (WarnEntry warnEntry : warnList) {
				if (warnEntry == warnEntryToRemove) {
					new BukkitRunnable() {
						@Override
						public void run() {
							ArrayList<Object> sqlParams = new ArrayList<>();
							sqlParams.add(warnEntry.id());
							storage.executeSafe("DELETE FROM playerwarns WHERE id=?", sqlParams);
						}
					}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
					warnList.remove(warnEntry);
					break;
				}
			}
		}

	}

	public static void showWarnsGUI(Player p) {
		PlayerData pd = PlayerData.get(p);
		WarnData wd = pd.warnData;
		if (!wd.isLoaded()) {
			p.sendMessage("Wystąpił nieoczekiwany błąd.");
			return;
		}
		wd.deleteExpired();
		WarnEntry entry;
		String guiTitle;
		guiTitle = String.format("&4&lOstrzeżenia %d/%d", wd.size(), WarnData.maxWarns);
		MagicGUI warnsGUI = MagicGUI.create(Utils.color(guiTitle), 9);
		warnsGUI.setAutoRemove(true);
		ItemStack glass = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
		for (int i = 0; i < wd.size(); i++) {
			entry = wd.getEntry(i);
			ArrayList<String> description = new ArrayList<>();
			description.add(Utils.color(String.format("&r&7Nadał/a: &3%s", entry.adminNick())));
			description.add(Utils.color(String.format("&r&7Wygasa za: &3%s", entry.getFormatExpire())));
			description.add(Utils.color(String.format("&r&7Data nadania: &3%s", entry.getFormatDate())));
			description.add(Utils.color(String.format("&r&7Powód: &3%s", entry.reason())));
			ItemStack warnItem = Utils.itemWithDisplayName(gVar.customItems.get("hWarning"), Utils.color("&r&4&lOstrzeżenie"), description);
			warnsGUI.setItem(i + 2, warnItem);
		}
		ArrayList<String> description = new ArrayList<>();
		description.add(Utils.color(String.format("&r&7Maksymalna ilość ostrzeżeń to &c%d&7.", WarnData.maxWarns)));
		description.add(Utils.color(String.format("&r&7Aktualnie posiadasz: &c%d&7.", wd.size())));
		description.add(Utils.color("&r&7Po otrzymaniu maksymalnej ilości ostrzeżeń"));
		description.add(Utils.color("&r&7zostaniesz &czbanowany/a na &l7 dni&7."));
		ItemStack infoItem = Utils.itemWithDisplayName(gVar.customItems.get("hInfo"), Utils.color("&r&e&lInformacja"), description);
		warnsGUI.setItem(0, infoItem);//Info
		warnsGUI.setItem(1, glass);
		warnsGUI.setItem(7, glass);
		ItemStack exitButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
		warnsGUI.setItem(8, exitButton, (player, gui, slot, type) -> {
			player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
			warnsGUI.close(player);
		});
		warnsGUI.open(p);
	}

	public static void showWarnListGUI(Player p, String targetName) {
		new BukkitRunnable() {
			@Override
			public void run() {
				WarnData wd = null;
				Player targetPlayer = Bukkit.getPlayer(targetName);
				if (targetPlayer == null) //player is offline
				{
					wd = new WarnData();
					wd.load(targetName);
					int hardBreak = 0;
					while (true) {//waiting for load
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						if (wd.isLoaded()) break;//data loaded - stop loop
						hardBreak++;
						if (hardBreak > 5) {
							print.error("Nieoczekiwany błąd warnListGUI -> Loading offline WarnData");
							break;
						}
					}
				} else {//player is online
					PlayerData pd = PlayerData.get(targetPlayer);
					wd = pd.warnData;
				}
				WarnData finalWd = wd;
				new BukkitRunnable() {
					@Override
					public void run() {
						if (finalWd.isLoaded()) {//WarnData is loaded
							finalWd.deleteExpired();
							String guiTitle;
							guiTitle = String.format("&4&lWarny: &4&l%s", targetName);
							MagicGUI warnListGUI = MagicGUI.create(Utils.color(guiTitle), 9);
							warnListGUI.setAutoRemove(true);
							WarnEntry entry = null;
							ItemStack glass = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
							for (int i = 0; i < finalWd.size(); i++) {
								entry = finalWd.getEntry(i);
								ArrayList<String> description = new ArrayList<>();
								description.add(Utils.color(String.format("&r&7Gracz: &3%s", entry.nick())));
								description.add(Utils.color(String.format("&r&7Nadał/a: &3%s", entry.adminNick())));
								description.add(Utils.color(String.format("&r&7Wygasa za: &3%s", entry.getFormatExpire())));
								description.add(Utils.color(String.format("&r&7Data nadania: &3%s", entry.getFormatDate())));
								description.add(Utils.color(String.format("&r&7Powód: &3%s", entry.reason())));
								if (p.hasPermission("eadventureplugin.unwarn")) {
									description.add(" ");
									description.add(Utils.color("&r&c&lSHIFT+PPM &7- aby usunąć ostrzeżenie."));
								}
								ItemStack warnItem = Utils.itemWithDisplayName(gVar.customItems.get("hWarning"), Utils.color("&r&4&lOstrzeżenie"), description);
								WarnEntry entryFinal = entry;
								warnListGUI.setItem(i + 2, warnItem, (player, gui, slot, type) -> {
									if (type == ClickType.SHIFT_RIGHT) {
										if (player.hasPermission("eadventureplugin.unwarn")) {
											player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
											finalWd.delete(entryFinal);
											warnListGUI.setItem(slot, new ItemStack(Material.AIR));
											ArrayList<String> info = new ArrayList<>();
											info.add(Utils.color(String.format("&r&7Maksymalna ilość ostrzeżeń to &c%d&7.", WarnData.maxWarns)));
											info.add(Utils.color(String.format("&r&7Aktualny stan to &c%d&7.", finalWd.size())));
											info.add(Utils.color("&r&7Po otrzymaniu maksymalnej ilości ostrzeżeń"));
											info.add(Utils.color("&r&7gracz zostaje &czbanowany/a na &l7 dni&7."));
											ItemStack infoItem = Utils.itemWithDisplayName(gVar.customItems.get("hInfo"), Utils.color("&r&e&lInformacja"), info);
											warnListGUI.setItem(0, infoItem);//Info
											p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&7Ostrzeżenie gracza &2%s &7zostało &2pomyślnie &7usunięte.", targetName)));
											if (targetPlayer != null) {
												targetPlayer.sendMessage(Utils.color("&2&lUsunięto Ci ostrzeżenie. &7Sprawdź &2&l/warns"));
												targetPlayer.sendMessage(Utils.color(String.format("&2Masz teraz &4%d/%d &2ostrzeżeń!", finalWd.size(), WarnData.maxWarns)));
											}
										}
									}
								});
							}
							ArrayList<String> info = new ArrayList<>();
							info.add(Utils.color(String.format("&r&7Maksymalna ilość ostrzeżeń to &c%d&7.", WarnData.maxWarns)));
							info.add(Utils.color(String.format("&r&7Aktualny stan to &c%d&7.", finalWd.size())));
							info.add(Utils.color("&r&7Po otrzymaniu maksymalnej ilości ostrzeżeń"));
							info.add(Utils.color("&r&7gracz zostaje &czbanowany/a na &l7 dni&7."));
							ItemStack infoItem = Utils.itemWithDisplayName(gVar.customItems.get("hInfo"), Utils.color("&r&e&lInformacja"), info);
							warnListGUI.setItem(0, infoItem);//Info
							warnListGUI.setItem(1, glass);
							warnListGUI.setItem(7, glass);
							ItemStack exitButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
							warnListGUI.setItem(8, exitButton, (player, gui, slot, type) -> {
								player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
								warnListGUI.close(player);
							});

							warnListGUI.open(p);
						} else {
							p.sendMessage("Wystąpił nieoczekiwany błąd (nie załadowano danych) - zgłoś to!");
						}
					}
				}.runTask(EternalAdventurePlugin.getInstance());
			}
		}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
	}

	public static void mutePlayer(String playerName, CommandSender admin, String reason, int timestampExpire) {
		int totalSeconds = timestampExpire - (int) Utils.getUnixTimestamp();
		int[] time = Utils.convertSecondsToTime(totalSeconds);

		String sql = "SELECT id FROM players WHERE nick=? AND mutedExpire < ?";
		ArrayList<Object> sqlParams = new ArrayList<>();
		sqlParams.add(playerName);
		sqlParams.add(Utils.getUnixTimestamp());
		storage.querySafe(sql, sqlParams, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			if (numRows != 0) {//player exist
				HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");
				int id = (int) row.get("id");
				String sqlExecute = "UPDATE players SET mutedExpire=?, mutedBy=?, mutedReason=? WHERE id=?";
				ArrayList<Object> sqlExecuteParams = new ArrayList<>();
				sqlExecuteParams.add(timestampExpire);
				sqlExecuteParams.add(admin.getName());
				sqlExecuteParams.add(reason);
				sqlExecuteParams.add(id);
				storage.executeSafe(sqlExecute, sqlExecuteParams);
				//notification
				notifyMessage(LogType.MUTE, playerName, admin.getName(), reason, timestampExpire + 1);//temp fix, add delay for mysql query +1 second
				//log
				String msg = String.format("%s. %dg, %dm, %ds.", reason, time[0], time[1], time[2]);
				log(LogType.MUTE, playerName, admin.getName(), msg);
				//if player is online
				Player targetPlayer = Bukkit.getPlayer(playerName);
				if (targetPlayer != null) {
					PlayerData pd = PlayerData.get(targetPlayer);
					pd.mutedExpire = timestampExpire;
					pd.mutedBy = admin.getName();
					pd.mutedReason = reason;
					msg = String.format("&c&lZostałeś/aś uciszony/a na &7%d godz, %d min, %d sek.", time[0], time[1], time[2]);
					targetPlayer.sendMessage(Utils.color(msg));
				}
				//message to admin
				admin.sendMessage(Utils.color(String.format("&2%s &7został/a &2pomyślnie &7uciszony/a.", playerName)));
			} else {
				admin.sendMessage(Utils.color(String.format("&2%s &7jest już uciszony/a.", playerName)));
			}
		});
	}

	public static void unmutePlayer(CommandSender adminSender, String targetName) {
		String sql = "SELECT id FROM players WHERE nick=? AND mutedExpire > ?";
		ArrayList<Object> sqlParams = new ArrayList<>();
		sqlParams.add(targetName);
		sqlParams.add(Utils.getUnixTimestamp());
		storage.querySafe(sql, sqlParams, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			if (numRows != 0) {
				HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");
				int id = (int) row.get("id");
				String sqlExecute = "UPDATE players SET mutedExpire=0, mutedBy=NULL, mutedReason=NULL WHERE id=" + id + ";";
				storage.execute(sqlExecute);
				String msg = String.format("&2%s &7został/a &2pomyślnie &7odciszony/a.", targetName);
				adminSender.sendMessage(Utils.color(msg));
				//if player is online
				Player targetPlayer = Bukkit.getPlayer(targetName);
				if (targetPlayer != null) {
					PlayerData pd = PlayerData.get(targetPlayer);
					pd.mutedExpire = 0;
					targetPlayer.sendMessage(Utils.color("&2&lZostałeś/aś odciszony/a."));
				}
			} else {
				adminSender.sendMessage(Utils.color("&7Ten gracz nie jest uciszony."));
			}
		});
	}

	public record MutedEntry(int id, String playerName, String adminName, String reason, int expiredTimestamp) {
		public String getFormatedExpire() {
			int secondRemaining = expiredTimestamp - (int) Utils.getUnixTimestamp();
			int[] time = Utils.convertSecondsToTime(secondRemaining);
			return String.format("%dg, %dm, %ds", time[0], time[1], time[2]);
		}
	}

	public static void showMutedPlayersGUI(Player p, int page, List<MutedEntry> list) {
		if (list == null) {//generate list
			String sql = "SELECT id, nick, mutedExpire, mutedBy, mutedReason" +
					" FROM players WHERE" +
					" mutedExpire > " + Utils.getUnixTimestamp() + ";";
			List<MutedEntry> mutedEntryList = new ArrayList<>();
			storage.query(sql, queryResult -> {
				int numRows = (int) queryResult.get("num_rows");
				@SuppressWarnings("unchecked")
				ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
				if (numRows > 0) {
					for (int i = 0; i < numRows; i++) {
						int id = (int) rows.get(i).get("id");
						String nick = (String) rows.get(i).get("nick");
						String mutedBy = (String) rows.get(i).get("mutedBy");
						String mutedReason = (String) rows.get(i).get("mutedReason");
						int mutedExpire = (int) rows.get(i).get("mutedExpire");
						mutedEntryList.add(new MutedEntry(id, nick, mutedBy, mutedReason, mutedExpire));
					}
				}
				/*for (int x = 0; x < 100; x++) {// Test pages
					mutedEntryList.add(new MutedEntry(x, "PlayerTest", "AdminTest", "Test", 1743465599));
				}*/
				showMutedPlayersGUI(p, 1, mutedEntryList);
			});
			return;
		}

		int itemsPerPage = 45;
		int startIndex = (page - 1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, list.size());
		int totalPages = (int) Math.ceil((double) list.size() / itemsPerPage);

		String guiTitle = String.format(Utils.color("&4&lUciszeni %d/%d"), page, Math.max(totalPages, 1));
		MagicGUI muteListGUI = MagicGUI.create(guiTitle, 54);
		muteListGUI.setAutoRemove(true);

		for (int i = startIndex; i < endIndex; i++) {
			MutedEntry me = list.get(i);
			ArrayList<String> description = new ArrayList<>();
			String strFormat = String.format("&r&7Gracz: &3%s", me.playerName());
			description.add(Utils.color(strFormat));
			strFormat = String.format("&r&7Nadał: &3%s", me.adminName());
			description.add(Utils.color(strFormat));
			strFormat = String.format("&r&7Wygasa za: &3%s", me.getFormatedExpire());
			description.add(Utils.color(strFormat));
			strFormat = String.format("&r&7Powód: &3%s", me.reason());
			description.add(Utils.color(strFormat));
			if (p.hasPermission("eadventureplugin.cmd.unmute")) {
				description.add(" ");
				description.add(Utils.color("&r&c&lSHIFT+PPM &7- aby odciszyć gracza."));
			}
			ItemStack playerHead = Utils.getPlayerHead(me.playerName(), Utils.color(String.format("&c&l%s", me.playerName())), description);
			muteListGUI.addItem(playerHead, (player, gui, slot, type) -> {
				if (type == ClickType.SHIFT_RIGHT) {
					if (player.hasPermission("eadventureplugin.cmd.unmute")) {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						unmutePlayer(player, me.playerName);
						muteListGUI.setItem(slot, new ItemStack(Material.AIR));
					}
				}
			});
		}
		muteListGUI.open(p);
		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					muteListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int previousPage = page - 1;
						showMutedPlayersGUI(player, previousPage, list);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					muteListGUI.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
				muteListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					muteListGUI.close(p);
				});
			} else if (x == 51) {
				if (endIndex < list.size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					muteListGUI.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						showMutedPlayersGUI(player, nextPage, list);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					muteListGUI.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				muteListGUI.setItem(x, navButton);
			}
		}
	}

	public static boolean isMuted(Player player) {
		PlayerData pd = PlayerData.get(player);
		return pd.mutedExpire > Utils.getUnixTimestamp();
	}

	public static void sendPlayerMutedInfo(Player player) {
		PlayerData pd = PlayerData.get(player);
		int muteSecondsRemaining = pd.mutedExpire - (int) Utils.getUnixTimestamp();
		int[] time = Utils.convertSecondsToTime(muteSecondsRemaining);
		String info = String.format("&4&lJesteś uciszony. Pozostało: &c%dg, %dm, %ds", time[0], time[1], time[2]);
		PlayerUtils.sendColorMessage(player, info);
	}
}
