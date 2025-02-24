package pl.eadventure.plugin.Modules;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.*;
import pl.eadventure.plugin.gVar;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnnounceManager {
	//TODO: ༺ ༻ ❌ ✔
	Plugin plugin;
	MySQLStorage storage;
	List<Announce> announceList = new ArrayList<>();
	ItemStack hGreenPlus;
	ItemStack hBlackX;
	ChatInputCapture chatInputCapture;

	//table: announcements
	static class Announce {
		static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		int id;
		String authorName;
		Timestamp created;
		Timestamp expire;
		Timestamp lastViewed;
		String text;

		public Announce(int id, String authorName, Timestamp created, Timestamp expire, Timestamp lastViewed, String text) {
			this.id = id;
			this.authorName = authorName;
			this.created = created;
			this.expire = expire;
			this.lastViewed = lastViewed;
			this.text = text;
		}

		public String getAuthorName() {
			return authorName;
		}

		public int getId() {
			return id;
		}

		public Timestamp getCreated() {
			return created;
		}

		public Timestamp getExpire() {
			return expire;
		}

		public Timestamp getLastViewed() {
			return lastViewed;
		}

		public String getExpireFormated() {
			return sdf.format(expire);
		}

		public String getCreatedFormated() {
			return sdf.format(created);
		}

		public String lastViewedFormated() {
			return sdf.format(lastViewed);
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setLastViewed(Timestamp lastViewed) {
			this.lastViewed = lastViewed;
		}
	}

	//constructor
	public AnnounceManager(Plugin plugin, MySQLStorage storage) {
		this.plugin = plugin;
		this.storage = storage;
		hGreenPlus = gVar.customItems.get("hGreenPlus");
		hBlackX = gVar.customItems.get("hBlackX");
		chatInputCapture = gVar.chatInputCapture;
		new BukkitRunnable() {
			@Override
			public void run() {
				sendBroadcast();
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 20L * 60L * 30L);
		load();
	}

	//load data from database
	public void load() {
		String sql = "SELECT " +
				"announcements.id, nick AS author, created, expire, lastviewed, text " +
				"FROM " +
				"announcements " +
				"LEFT JOIN players ON announcements.author=players.id " +
				"ORDER BY expire DESC;";
		print.debug(sql);
		storage.query(sql, queryResult -> {
			print.debug("Dane: " + queryResult);
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");

			if (numRows > 0) {
				announceList.clear();
				int id;
				String author;
				Timestamp created;
				Timestamp expire;
				Timestamp lastViewed;
				String text;
				for (int i = 0; i < numRows; i++) {
					id = (int) rows.get(i).get("id");
					author = (String) rows.get(i).get("author");
					created = (Timestamp) rows.get(i).get("created");
					expire = (Timestamp) rows.get(i).get("expire");
					lastViewed = (Timestamp) rows.get(i).get("lastviewed");
					text = (String) rows.get(i).get("text");
					announceList.add(new Announce(id, author, created, expire, lastViewed, text));
				}
				sendBroadcast();
			}
		});

	}

	//show main menu
	public void showMainMenuGUI(Player p) {
		final boolean canManage = p.hasPermission("eadventureplugin.annuance.manage");
		//final boolean canManage = false;
		MagicGUI mainGui = MagicGUI.create(Utils.mm("<bold><gradient:#BF00FF:#7d00a7>Ogłoszenia</bold>"), 54);
		mainGui.setAutoRemove(true);
		// Annuance list
		int annInGUISlot = 0;
		for (Announce ann : announceList) {
			if (ann.getExpire().before(Timestamp.from(Instant.now()))) continue;// Don't show expired anns
			ArrayList<Component> lore = new ArrayList<>();
			String text = ann.text;
			final int maxLineLength = 40;

			List<String> textLines = Utils.breakLinesWithTags(text, maxLineLength);

			for (String textLine : textLines) {
				lore.add(Utils.mm("<!i><#FFFFFF>" + textLine));
			}
			String color = "#740DC6";
			//lore.add(Utils.mm("<!i><#FFFFFF>" + ann.text));
			lore.add(Utils.mm(""));
			lore.add(Utils.mm("<!i><gradient:#730088:#740DC6>Dodał:</gradient> <gray><bold>" + ann.getAuthorName()));
			lore.add(Utils.mm("<!i><gradient:#730088:#740DC6>Ostatnio wyświetlono:</gradient> <gray><bold>" + ann.lastViewedFormated()));
			lore.add(Utils.mm("<!i><gradient:#730088:#740DC6>Utworzono:</gradient> <gray><bold>" + ann.getCreatedFormated()));
			lore.add(Utils.mm("<!i><gradient:#730088:#740DC6>Wygasa:</gradient> <gray><bold>" + ann.getExpireFormated()));
			if (canManage) {
				lore.add(Utils.mm(""));
				lore.add(Utils.mm("<!i><bold><#56c61a>LPM <gray>- Wyświetl podgląd na czacie."));
				lore.add(Utils.mm("<!i><bold><#56c61a>SHIFT+PPM <gray>- Usuń ogłoszenie."));
				lore.add(Utils.mm("<!i><bold><#56c61a>SHIFT+LPM <gray>- Edytuj treść ogłoszenia."));
			}


			ItemStack annItem = Utils.itemWithDisplayName(ItemStack.of(Material.BLUE_BANNER), Utils.mm("<!i><bold><#999999>Ogłoszenie</bold>"), lore);
			mainGui.setItem(annInGUISlot, annItem, ((player, gui, slot, type) -> {
				if (!canManage) return;
				if (type == ClickType.LEFT) {
					player.sendMessage(Utils.mm(ann.text));
				} else if (type == ClickType.SHIFT_RIGHT) {//Delete ann
					if (announceList.contains(ann)) {
						String sqlDelete = "DELETE FROM announcements WHERE id=" + ann.id;
						storage.execute(sqlDelete);
						announceList.remove(ann);
						showMainMenuGUI(player);
					}
				} else if (type == ClickType.SHIFT_LEFT) {//Edit ann
					player.sendMessage(Utils.mm("<gradient:#a500d3:#440057><strikethrough>༺-------------------------------------------------༻</gradient>"));
					player.sendMessage(Utils.mm(ann.text));
					List<Component> messages = new ArrayList<>();
					messages.add(Utils.mm("<bold><#FF0000><underlined><click:SUGGEST_COMMAND:'" + ann.text + "'>✎ Kliknij tutaj aby edytować ✎"));
					messages.add(Utils.mm("<#AAAAAA><bold>Lub wprowadź nową treść na czacie."));
					messages.add(Utils.mm("<#AAAAAA><bold>Aby anulować wpisz: <#FF0000>anuluj"));
					messages.add(Utils.mm("<#AAAAAA><bold>Aby zapisać wpisz: <#FF0000>zapisz"));
					final String backup = ann.text;
					chatInputCapture.receiveInput(player, messages, (playerInput, message) -> {
						if (message.equalsIgnoreCase("anuluj")) {
							ann.setText(backup);
							showMainMenuGUI(playerInput);
							return 1;
						} else if (message.equalsIgnoreCase("zapisz")) {
							String saveTextSql = "UPDATE announcements SET text=? WHERE id=?;";
							ArrayList<Object> parameters = new ArrayList<>();
							parameters.add(ann.getText());
							parameters.add(ann.id);
							storage.executeSafe(saveTextSql, parameters);
							playerInput.sendMessage(Utils.mm("<#FF0000><bold>Zapisano:"));
							playerInput.sendMessage(Utils.mm(ann.getText()));
							return 1;
						} else {
							ann.setText(message);

							playerInput.sendMessage("");
							playerInput.sendMessage(Utils.mm(message));
							playerInput.sendMessage("");
							playerInput.sendMessage(Utils.mm("<#AAAAAA><bold>Aby anulować wpisz: <#FF0000>anuluj"));
							playerInput.sendMessage(Utils.mm("<#AAAAAA><bold>Aby zapisać wpisz: <#FF0000>zapisz"));
							return 0;
						}
					});
				}
			}));
			annInGUISlot++;
			if (annInGUISlot > 45) break;
		}
		//()//blokowanie kompilacji
		// Pane GUI
		mainGui.setItem(46, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(47, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(48, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(50, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(51, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(52, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		mainGui.setItem(53, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		// Buttons
		ItemStack buttonClose = Utils.itemWithDisplayName(hBlackX, Utils.mm("<!i><bold><#999999>Zamknij</#999999></bold>"), null);
		mainGui.setItem(49, buttonClose, ((playerGui, gui, slot, type) -> {
			mainGui.close(playerGui);
		}));
		ItemStack buttonAdd = Utils.itemWithDisplayName(hGreenPlus, Utils.mm("<!i><bold><#999999>Dodaj nowy wpis</#999999></bold>"), null);
		mainGui.setItem(45, buttonAdd, ((playerGui, gui, slot, type) -> {
			mainGui.close(playerGui);
			List<Component> messages = new ArrayList<>();
			//Get annuance text
			//<gradient:#730088:#FFFFFF>
			messages.add(Utils.mm("<#AAAAAA><bold>Wprowadź treść ogłoszenia na czacie."));
			messages.add(Utils.mm("<#AAAAAA><bold>Używaj formatu <#FF0000>" +
					"<underlined><hover:show_text:'<color:#37ff00>Kliknij aby otworzyć narzędzie<br>do formatowania kodu.</color>'>" +
					"<click:OPEN_URL:'https://webui.advntr.dev/'>MiniMessage</click>" +
					"</hover></underlined>"));
			messages.add(Utils.mm("<#AAAAAA><bold>Aby anulować wpisz: <#FF0000>anuluj"));
			chatInputCapture.receiveInput(playerGui, messages, (playerInput, message) -> {
				if (message.equalsIgnoreCase("anuluj")) {
					showMainMenuGUI(playerInput);
					return 1;
				}
				String announceInput = message;
				p.sendMessage(Utils.mm("<#00FF00><bold><underlined>Podgląd ogłoszenia:"));
				p.sendMessage(Utils.mm(announceInput));
				// Get end date
				messages.clear();

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
				messages.add(Utils.mm("<#AAAAAA><bold>Wprowadź datę i godzinę zakończenia."));
				messages.add(Utils.mm("<#AAAAAA><bold>Użyj formatu: <#FF0000>DD-MM-RRRR GG:MM"));
				messages.add(Utils.mm("<#AAAAAA><bold>Aby anulować wpisz: <#FF0000>anuluj"));
				print.debug("New capture");
				chatInputCapture.receiveInput(playerGui, messages, (playerInput2, message2) -> {
					if (message2.equalsIgnoreCase("anuluj")) {
						showMainMenuGUI(playerInput);
						return 1;
					}
					String dateInput = message2;
					LocalDateTime date;
					try {
						date = LocalDateTime.parse(dateInput, formatter);
					} catch (Exception e) {
						p.sendMessage(Utils.mm("<#FF0000>Nieprawidłowa data i godzina."));
						return 0;
					}
					if (LocalDateTime.now().isAfter(date)) {
						p.sendMessage(Utils.mm("<#FF0000>Podany termin już upłynął. Podaj termin z przyszłości :)"));
						return 0;
					}
					// Insert to database

					// Show summary
					///p.sendMessage("Podsumowanie:");
					//p.sendMessage(Utils.mm(announceInput));
					//p.sendMessage("Termin ważności: " + dateInput);
					// Show main menu
					//showMainMenuGUI(playerInput);
					ArrayList<Object> parameters = new ArrayList<>();
					PlayerData pd = PlayerData.get(playerInput2);
					String insertSql = "INSERT INTO announcements (author, created, expire, lastviewed, text) VALUES (?, ?, ?, ?, ?);";
					parameters.add(pd.dbid);
					Timestamp created = Timestamp.from(Instant.now());
					parameters.add(created);
					Instant instantExpire = date.atZone(ZoneId.systemDefault()).toInstant();
					Timestamp expire = Timestamp.from(instantExpire);
					parameters.add(expire);
					parameters.add(created);
					parameters.add(announceInput);
					int insertId = storage.executeGetInsertID(insertSql, parameters);
					Announce ann = new Announce(insertId, playerInput2.getName(), created, expire, created, announceInput);
					announceList.add(ann);
					showMainMenuGUI(playerInput2);
					return 1;
				});
				return 0;
			});
		}));

		if (!canManage) {
			mainGui.setItem(45, ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
		}
		mainGui.open(p);
	}

	// Send broadcast
	private void sendBroadcast() {
		Optional<Announce> oldestAnnounce = announceList.stream()
				.filter(a -> a.getLastViewed() != null)
				.filter(a -> a.getExpire().after(Timestamp.from(Instant.now())))
				.min(Comparator.comparing(Announce::getLastViewed));
		if (oldestAnnounce.isPresent()) {
			Announce announce = oldestAnnounce.get();
			announce.setLastViewed(Timestamp.from(Instant.now()));
			String saveTextSql = "UPDATE announcements SET lastviewed=? WHERE id=?;";
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add(announce.getLastViewed());
			parameters.add(announce.id);
			storage.executeSafe(saveTextSql, parameters);
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.sendMessage(Utils.mm("" +
						"<gradient:#a500d3:#440057><bold>༺</bold><strikethrough>" +
						"-------------------------------------------------" +
						"</strikethrough><bold>༻</bold></gradient>"));
				player.sendMessage(Utils.mm(announce.getText()));
				Location playerLocation = player.getLocation();
				player.playSound(playerLocation, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1.0f, 1.5f);
				player.sendMessage(Utils.mm("" +
						"<gradient:#a500d3:#440057><bold>༺</bold><strikethrough>" +
						"-------------------------------------------------" +
						"</strikethrough><bold>༻</bold></gradient>"));
			}
		}
	}
}
