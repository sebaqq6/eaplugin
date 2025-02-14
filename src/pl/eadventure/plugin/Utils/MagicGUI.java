package pl.eadventure.plugin.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class MagicGUI {

	private static MagicGUIListener listener;
	private static final Map<UUID, MagicGUI> guis = new HashMap<>();
	private static final Map<UUID, UUID> actives = new HashMap<>();
	private final Map<Integer, ClickAction> actions = new HashMap<>();
	private final Set<Player> viewers = new HashSet<>();
	private final UUID uuid;
	private final Inventory inventory;
	private final Component title;
	private boolean autoRemove;
	private final int size;

	public static MagicGUI create(String title, int size) {
		switch (size) {
			case 54, 45, 36, 27, 18, 9 -> {
			}
			default -> size = 9;
		}

		return new MagicGUI(LegacyComponentSerializer.legacy('&').deserialize(title), size);
	}

	public static MagicGUI create(Component title, int size) {
		switch (size) {
			case 54, 45, 36, 27, 18, 9 -> {
			}
			default -> size = 9;
		}
		return new MagicGUI(title, size);
	}

	public static MagicGUI getFromId(UUID menuId) {
		return guis.get(menuId);
	}

	public static MagicGUI getFromPlayer(Player player) {
		UUID menuId = actives.get(player.getUniqueId());
		if (menuId == null) return null;
		return guis.get(menuId);
	}

	public static boolean isInMenu(Player player) {
		return actives.containsKey(player.getUniqueId());
	}

	public static boolean isLoaded() {
		return listener != null;
	}

	public static void tryToLoadFor(Plugin plugin) {
		if (listener == null) {
			listener = new MagicGUIListener();
			Bukkit.getPluginManager().registerEvents(listener, plugin);
		}
	}

	public static void tryToUnload() {
		if (listener != null) {
			HandlerList.unregisterAll(listener);
		}
	}

	private MagicGUI(Component title, int size) {
		this.title = title;
		this.size = size;
		this.uuid = UUID.randomUUID();
		this.inventory = Bukkit.createInventory(null, size, title);
		guis.put(this.uuid, this);
	}


	public void setAutoRemove(boolean autoRemove) {
		this.autoRemove = autoRemove;
	}

	public void addItem(ItemStack itemStack) {
		addItem(itemStack, (player, gui, slot, type) -> {
		});
	}

	public void addItem(ItemStack itemStack, ClickAction clickAction) {
		int slot = actions.size();
		if (slot < size) {
			actions.put(slot, clickAction);
			inventory.setItem(slot, itemStack);
		}
	}

	public void setItem(int slot, ItemStack itemStack) {
		setItem(slot, itemStack, (player, gui, slot1, type) -> {
		});
	}

	public void setItem(int slot, ItemStack itemStack, ClickAction clickAction) {
		if (slot < size) {
			actions.put(slot, clickAction);
			inventory.setItem(slot, itemStack);
		}
	}

	public void setUpdateItem(int slot, ItemStack itemStack) {
		if (slot < size) {
			inventory.setItem(slot, itemStack);
		}
	}

	public void open(Player player) {
		player.openInventory(inventory);
		processOpen(player);
	}

	public void open(Player... players) {
		for (Player var : players) {
			open(var);
		}
	}

	public void openForAll() {
		for (Player var : Bukkit.getOnlinePlayers()) {
			open(var);
		}
	}

	public void closeForAll() {
		for (Player var : viewers) {
			close(var);
		}
	}

	public void close(Player... players) {
		for (Player var : players) {
			close(var);
		}
	}

	public void close(Player player) {
		player.closeInventory();
		processClose(player);
        /*if (viewers.isEmpty() && autoRemove) {
            //guis.remove(this.uuid);
            unregister();
        }*/
	}

	public void unregister() {
		print.debug("MagicGUI->Unregister: " + this.getTitle());
		guis.remove(this.uuid);
	}

	public void update() {
		for (Player var : viewers) {
			var.updateInventory();
		}
	}

	private void processClick(Player player, ClickType clickType, int slot) {
		ClickAction action = actions.get(slot);
		if (action != null) {
			action.onClick(player, this, slot, clickType);
		}
	}

	private void processOpen(Player player) {
		actives.put(player.getUniqueId(), this.uuid);
		viewers.add(player);
	}

	private void processClose(Player player) {
		actives.remove(player.getUniqueId());
		viewers.remove(player);
	}

	public static MagicGUIListener getListener() {
		return listener;
	}

	public Map<Integer, ClickAction> getActions() {
		return actions;
	}

	public Set<Player> getViewers() {
		return viewers;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public String getTitle() {
		return PlainTextComponentSerializer.plainText().serialize(title);
	}

	public boolean isAutoRemove() {
		return autoRemove;
	}

	public int getSize() {
		return size;
	}

	public interface ClickAction {
		void onClick(Player player, MagicGUI gui, int slot, ClickType type);
	}

	private static class MagicGUIListener implements Listener {

		@EventHandler
		public void onClick(InventoryClickEvent e) {
			UUID clicker = e.getWhoClicked().getUniqueId();
			if (actives.containsKey(clicker)) {
				UUID menu = actives.get(clicker);
				MagicGUI gui = guis.get(menu);
				if (gui != null) {
					e.setCancelled(true);
					Player player = (Player) e.getWhoClicked();
					gui.processClick(player, e.getClick(), e.getRawSlot());
					//print.error(String.format("Gracz: %s -> click: %s -> rawSlot: %d", player.getName(), e.getClick().toString(), e.getRawSlot()));
				} else {
					actives.remove(clicker);
				}
			}
		}

		@EventHandler
		public void onQuit(InventoryCloseEvent e) {
			UUID closer = e.getPlayer().getUniqueId();
			if (actives.containsKey(closer)) {
				UUID menu = actives.remove(closer);
				MagicGUI gui = guis.get(menu);
				if (gui != null) {
					//print.debug("InventoryCloseEvent -> MagicGUI: "+gui.title);
					gui.processClose((Player) e.getPlayer());
					if (gui.viewers.isEmpty() && gui.autoRemove) {
						//guis.remove(this.uuid);
						gui.unregister();
					}
				}
			}
		}

	}

}