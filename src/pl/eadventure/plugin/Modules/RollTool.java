package pl.eadventure.plugin.Modules;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Utils.print;

import java.util.HashMap;
import java.util.Map;

public class RollTool {
	static Plugin plugin;
	private final HashMap<String, Integer> rollList = new HashMap<>();
	private boolean registerRolls;

	public RollTool() {
		this.registerRolls = false;
	}

	public static void loadRollTool(Plugin instance) {
		plugin = instance;
	}

	public boolean isRegisterRolls() {
		return registerRolls;
	}

	public void addRoll(String nick, int val) {//dodawanie liczb
		if (!this.registerRolls) return;
		if (!rollList.containsKey(nick)) {
			rollList.put(nick, val);
		}
	}

	public void startRegisterRolls(int seconds, RollResult callback) {
		if (this.registerRolls) return;
		this.registerRolls = true;
		rollList.clear();
		if (seconds < 1) seconds = 1;

		new BukkitRunnable() {
			@Override
			public void run() {
				HashMap<String, Integer> bestRolls = new HashMap<>();
				int highestRoll = -1;

				for (Map.Entry<String, Integer> entry : rollList.entrySet()) {
					int roll = entry.getValue();
					String playerName = entry.getKey();

					if (roll > highestRoll) {
						highestRoll = roll;
						bestRolls.clear();
						bestRolls.put(playerName, roll);
					} else if (roll == highestRoll) {
						bestRolls.put(playerName, roll);
					}
				}
				callback.onRollingEnd(bestRolls);
				registerRolls = false;
			}
		}.runTaskLaterAsynchronously(plugin, 20L * seconds);
	}

	public static void test() {
		RollTool rt = new RollTool();
		rt.startRegisterRolls(1, result -> {
			print.info("Wyniki:");
			for (Map.Entry<String, Integer> entry : result.entrySet()) {
				print.info("Wygrał los gracza " + entry.getKey() + ", wynik: " + entry.getValue());
			}
		});
		rt.addRoll("Gracz1", 20);
		rt.addRoll("Gracz2", 52);
		rt.addRoll("Gracz3", 21);
		rt.addRoll("Gracz4", 50);
		rt.addRoll("Gracz4", 90);//player4 try cheat?
	}

	public interface RollResult {
		void onRollingEnd(HashMap<String, Integer> bestRolls);
	}
}
