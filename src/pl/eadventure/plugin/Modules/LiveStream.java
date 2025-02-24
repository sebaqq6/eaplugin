package pl.eadventure.plugin.Modules;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.ChatInputCapture;
import pl.eadventure.plugin.Utils.MagicGUI;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LiveStream {
	//trash code ...
	MySQLStorage storage = EternalAdventurePlugin.getMySQL();
	ChatInputCapture chatInputCapture = gVar.chatInputCapture;
	static ItemStack hBlackX = gVar.customItems.get("hBlackX");
	static ItemStack rainbowDiamond = gVar.customItems.get("rainbowDiamond");
	static int announceTimer = 0;

	public LiveStream() {
		new BukkitRunnable() {
			@Override
			public void run() {
				oneSecondTimer();
			}
		}.runTaskTimerAsynchronously(EternalAdventurePlugin.getInstance(), 20L, 20L);
	}

	public void showGui(Player player) {
		MagicGUI mainGui = MagicGUI.create(Utils.mm("<bold><gradient:#BF00FF:#7d00a7>Reżyserka</bold>"), 54);
		PlayerData pd = PlayerData.get(player);
		//3 7 12 14 20, 21, 22, 23, 24 29 33, 38 39 40 41 42
		for (int x = 0; x < 54; x++) {
			if (x == 2 || x == 6 || x == 12 || x == 14 || (x >= 20 && x <= 24) || x == 29 || x == 33 || x == 38 || x == 39 || x == 40 || x == 41 || x == 42) {
				mainGui.setItem(x, Utils.itemWithDisplayName(ItemStack.of(Material.RED_STAINED_GLASS_PANE), " ", null));
			} else {
				mainGui.setItem(x, Utils.itemWithDisplayName(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE), " ", null));
			}
		}
		//30 - Edit stream link
		ArrayList<Component> urlEditLore = new ArrayList<>();
		urlEditLore.add(Utils.mm("<!i><gradient:#BF00FF:#7d00a7>Kliknij tutaj aby edytować"));
		urlEditLore.add(Utils.mm("<!i><gradient:#BF00FF:#7d00a7>link do swojej transmisji na żywo."));
		urlEditLore.add(Utils.mm(""));
		String sa = "#BF00FF";
		urlEditLore.add(Utils.mm("<!i><#730088><bold>Aktualnie ustawiony link:"));
		urlEditLore.add(Utils.mm("<gradient:#0028FF:#00CFFF>" + pd.streamerURL));
		ItemStack editUrl = Utils.itemWithDisplayName(ItemStack.of(Material.MAP), Utils.mm("" +
				"<!i><bold><#999999>Edytuj link</bold>" +
				""), urlEditLore);
		mainGui.setItem(30, editUrl, ((player1, gui, slot, type) -> {
			mainGui.close(player1);
			List<Component> messages = new ArrayList<>();
			messages.add(Utils.mm("<#AAAAAA><bold>Wklej link na czacie."));
			messages.add(Utils.mm("<#AAAAAA><bold>Użyj <#FF0000>CTRL+V"));
			messages.add(Utils.mm("<#AAAAAA><bold>Aby anulować wpisz: <#FF0000>anuluj"));
			chatInputCapture.receiveInput(player1, messages, (playerInput, message) -> {
				if (message.equalsIgnoreCase("anuluj")) {
					this.showGui(playerInput);
					return 1;
				}
				pd.streamerURL = message;
				ArrayList<Object> params = new ArrayList<>();
				params.add(pd.streamerURL);
				params.add(pd.dbid);
				storage.executeSafe("UPDATE players SET streamer_url=? WHERE id=?", params);
				playerInput.sendMessage(Utils.mm("<#AAAAAA><bold>Ustawiłeś/aś link do Twojej transmisji na żywo:"));
				playerInput.sendMessage(Utils.mm("" +
						"<underlined><hover:show_text:'<color:#37ff00>Kliknij aby otworzyć link.</color>'>" +
						"<click:OPEN_URL:'" + message + "'>" + message + "</click>" +
						"</hover></underlined>"));
				return 1;
			});
		}));
		//Get reward
		ArrayList<Component> rewardLore = new ArrayList<>();
		rewardLore.add(Utils.mm("<!i><#56c61a><bold>Kliknij, by odebrać nagrodę."));
		rewardLore.add(Utils.mm(""));
		rewardLore.add(Utils.mm("<!i><#56c61a>Dziękujemy za twoje zaangażowanie,"));
		rewardLore.add(Utils.mm("<!i><#56c61a>Twoje wsparcie napędza tę przygodę!"));
		rewardLore.add(Utils.mm(""));
		rewardLore.add(Utils.mm("<!i><red><bold>Dostępne raz na 12 godzin."));
		ItemStack getReward = Utils.itemWithDisplayName(rainbowDiamond, Utils.mm("<gradient:#EB0012:#BF00FF><bold>Odbierz nagrodę"), rewardLore);
		mainGui.setItem(31, getReward, ((player1, gui, slot, type) -> {
			player1.chat("/playerhiddencmdstreamer");

		}));
		//Start/Stop live
		mainGui.setItem(32, liveStatusIcon(pd.onLiveStream), ((player1, gui, slot, type) -> {
			if (pd.onLiveStream) {
				pd.onLiveStream = false;
				Bukkit.broadcast(Utils.mm(String.format("" +
						"<#AAAAAA><bold>Gracz <#00FF00>%s<#AAAAAA> zakończył transmisje na żywo.", player1.getName())));
			} else {
				pd.onLiveStream = true;
				String link = pd.streamerURL;
				Bukkit.broadcast(Utils.mm(String.format("" +
						"<hover:show_text:'<color:#37ff00>Kliknij tutaj aby obejrzeć transmisje na żywo.</color>'>" +
						"<click:OPEN_URL:'" + link + "'><#FF0000><bold>Gracz <#00FF00>%s<#FF0000> rozpoczął transmisje na żywo. <yellow><underlined>Kliknij tutaj aby obejrzeć.</underlined></click>" +
						"</hover>", player1.getName())));
				mainGui.close(player1);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound my_sounds:sounds.yeah.scream ambient @p ~ ~ ~ 2 1");
			}
			mainGui.setUpdateItem(32, liveStatusIcon(pd.onLiveStream));
		}));
		ItemStack exitButton = Utils.itemWithDisplayName(hBlackX, Utils.color("&r&7&lZamknij"), null);
		mainGui.setItem(49, exitButton, ((player1, gui, slot, type) -> {
			mainGui.close(player1);
		}));
		mainGui.setAutoRemove(true);
		mainGui.open(player);
	}

	public void addStreamer(Player player) {
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(player.getName());
		storage.executeSafe("UPDATE players SET streamer=1, streamer_service='Brak', streamer_url='Brak' WHERE nick=?", parameters);
		PlayerData pd = PlayerData.get(player);
		pd.isStreamer = 1;
		pd.streamerService = "Brak";
		pd.streamerURL = "Brak";
	}

	public void delStreamer(String nick) {
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(nick);
		storage.executeSafe("UPDATE players SET streamer=0, streamer_service='Brak', streamer_url='Brak' WHERE nick=?", parameters);
		Player player = Bukkit.getPlayer(nick);
		if (player != null && player.isOnline()) {
			PlayerData pd = PlayerData.get(player);
			pd.isStreamer = 0;
			pd.streamerService = "Brak";
			pd.streamerURL = "Brak";
		}
	}

	int livePlaceholderStep = 0;
	String livePlaceHolder = null;

	private void oneSecondTimer() {
		if (announceTimer <= 0) {
			announceStream();
			announceTimer = 10;
		} else {
			announceTimer--;
		}
		if (livePlaceholderStep == 0) {
			livePlaceholderStep = 1;
			livePlaceHolder = "#FF0000&lʟɪᴠᴇ #FF0000\uD83C\uDFA5";
		} else {
			livePlaceholderStep = 0;
			livePlaceHolder = "&8&lʟɪᴠᴇ &8\uD83C\uDFA5";
		}
	}

	public String getLivePlaceholder() {
		return livePlaceHolder;
	}

	private ItemStack liveStatusIcon(boolean status) {
		ArrayList<Component> startStopStreamLore = new ArrayList<>();
		ItemStack startStopStream = null;
		if (status) {
			startStopStreamLore.add(Utils.mm("<!i><gradient:#BF00FF:#7d00a7>Kliknij aby zatrzymać transmisję na żywo."));
			startStopStream = Utils.itemWithDisplayName(ItemStack.of(Material.REDSTONE), Utils.mm("<!i><bold><#FF0000>Zatrzymaj transmisje</bold>"), startStopStreamLore);
		} else {
			startStopStreamLore.add(Utils.mm("<!i><gradient:#BF00FF:#7d00a7>Kliknij aby rozpocząć transmisję na żywo."));
			startStopStream = Utils.itemWithDisplayName(ItemStack.of(Material.GUNPOWDER), Utils.mm("<!i><bold><#00FF00>Rozpocznij transmisje</bold>"), startStopStreamLore);
		}
		return startStopStream;
	}

	public void showStreamersList(Player player) {
		String sql = "SELECT nick FROM players WHERE streamer=1";
		storage.query(sql, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				player.sendMessage(Utils.mm("<#FF0000>---------------------------"));
				for (int i = 0; i < numRows; i++) {
					String nick = (String) rows.get(i).get("nick");
					player.sendMessage(nick);

				}
				player.sendMessage(Utils.mm("<#FF0000>---------------------------"));
			}
		});
	}

	private void announceStream() {
		//print.error("announceStream");
	}
}
