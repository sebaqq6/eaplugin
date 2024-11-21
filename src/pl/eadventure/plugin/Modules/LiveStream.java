package pl.eadventure.plugin.Modules;

import org.bukkit.entity.Player;
import pl.eadventure.plugin.Utils.MagicGUI;

public class LiveStream {
	public LiveStream() {

	}

	public void load() {

	}

	public void showGui(Player player) {
		MagicGUI mainGui = MagicGUI.create("Panel streamera", 54);
		mainGui.setAutoRemove(true);
		mainGui.open(player);
	}
}
