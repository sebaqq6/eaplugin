package pl.eadventure.plugin.Modules;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.Utils.*;
import pl.eadventure.plugin.gVar;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AnnounceManager {
	Plugin plugin;
	MySQLStorage storage;
	List<Announce> announceList = new ArrayList<>();
	ItemStack hGreenPlus;
	ItemStack hBlackX;
	ChatInputCapture chatInputCapture;

	//table: announcements
	record Announce(int id, String authorName, Timestamp created, Timestamp expire, String text) {
	}

	//constructor
	public AnnounceManager(Plugin plugin, MySQLStorage storage) {
		this.plugin = plugin;
		this.storage = storage;
		hGreenPlus = gVar.customItems.get("hGreenPlus");
		hBlackX = gVar.customItems.get("hBlackX");
		chatInputCapture = new ChatInputCapture(plugin);
		load();
	}

	//load data from database
	public void load() {
		String sql = "SELECT " +
				"announcements.id, nick AS author, created, expire, text " +
				"FROM " +
				"announcements " +
				"LEFT JOIN players ON announcements.author=players.id " +
				"ORDER BY expire DESC;";
		storage.query(sql, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				announceList.clear();
				int id;
				String author;
				Timestamp created;
				Timestamp expire;
				String text;
				for (int i = 0; i < numRows; i++) {
					id = (int) rows.get(i).get("id");
					author = (String) rows.get(i).get("author");
					created = (Timestamp) rows.get(i).get("created");
					expire = (Timestamp) rows.get(i).get("expire");
					text = (String) rows.get(i).get("text");
					announceList.add(new Announce(id, author, created, expire, text));
				}
			}
		});
	}

	//show main menu
	public void showMainMenuGUI(Player p) {
		boolean canManage = p.hasPermission("eadventureplugin.annuance.manage");
		MagicGUI mainGui = MagicGUI.create(Utils.mm("<bold><gradient:#BF00FF:#7d00a7>Ogłoszenia</bold>"), 54);
		mainGui.setAutoRemove(true);
		ItemStack buttonClose = Utils.itemWithDisplayName(hBlackX, Utils.mm("<!i><bold><#999999>Zamknij</#999999></bold>"), null);
		mainGui.setItem(49, buttonClose, ((playerGui, gui, slot, type) -> {
			mainGui.close(playerGui);
		}));
		ItemStack buttonAdd = Utils.itemWithDisplayName(hGreenPlus, Utils.mm("<!i><bold><#999999>Dodaj nowy wpis</#999999></bold>"), null);
		mainGui.setItem(48, buttonAdd, ((playerGui, gui, slot, type) -> {
			mainGui.close(playerGui);
			List<Component> messages = new ArrayList<>();
			//Get annuance text
			messages.add(Utils.mm("Wprowadź treść ogłoszenia na czacie."));
			messages.add(Utils.mm("Możesz używać technologi MiniMessage"));
			messages.add(Utils.mm("Aby anulować wpisz: anuluj"));
			chatInputCapture.receiveInput(playerGui, messages, (playerInput, message) -> {
				if (message.equalsIgnoreCase("anuluj")) {
					showMainMenuGUI(playerInput);
					return 1;
				}
				String annuanceInput = message;
				// Get end date
				messages.clear();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
				messages.add(Utils.mm("Wprowadź datę i godzinie zakończenia."));
				messages.add(Utils.mm("Użyj formatu: DD-MM-RRRR GG:MM"));
				messages.add(Utils.mm("Aby anulować wpisz: anuluj"));
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
					//p.sendMessage("Podsumowanie:");
					//p.sendMessage(Utils.mm(annuanceInput));
					//p.sendMessage("Termin ważności: " + dateInput);
					// Show main menu
					showMainMenuGUI(playerInput);
					return 1;
				});
				return 0;
			});
		}));
		mainGui.open(p);
	}
}
