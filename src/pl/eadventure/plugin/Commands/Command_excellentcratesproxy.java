package pl.eadventure.plugin.Commands;

import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.framework.Arena;
import com.garbagemule.MobArena.framework.ArenaMaster;
import com.garbagemule.MobArena.things.Thing;
import com.garbagemule.MobArena.things.ThingPicker;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.print;

public class Command_excellentcratesproxy implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		StringBuilder fullCommand = new StringBuilder(label);
		for (String arg : args) {
			fullCommand.append(" ").append(arg);
		}
		String proxyCommand = fullCommand.toString().replaceAll(label, "excellentcrates");
		Bukkit.dispatchCommand(sender, proxyCommand);
		//add key to players reward on mobarena
		if (args.length < 3) return true;
		if (!args[0].equalsIgnoreCase("key")) return true;
		MobArena ma = EternalAdventurePlugin.getMobArena();
		ArenaMaster am = ma.getArenaMaster();
		if (args[1].equalsIgnoreCase("give")) {///excellentcrates key[0] give[1] JrDesmond[2] epic[3] 1[4] (5)
			if (args.length < 5) return true;
			String key = args[3];
			String keyCount = args[4];
			Player targetPlayer = Bukkit.getPlayer(args[2]);
			if (targetPlayer == null) return true;
			Arena arena = am.getArenaWithPlayer(targetPlayer);
			//print.debug("Arena: " + arena);

			if (arena == null) return true;
			//print.debug("arena.getPlayersInArena().contains(targetPlayer): " + arena.getAllPlayers().contains(targetPlayer));
			//print.debug("arena.isRunning(): " + arena.isRunning());
			if (!arena.getAllPlayers().contains(targetPlayer)) return true;
			setAllReadyOnMA(arena);
			if (!arena.isRunning()) {
				arena.forceStart();
			}
			String thingInput = String.format("cmd(Klucz: %s):/excellentcrates key give <player> %s %s", key, key, keyCount);
			Thing thing;
			try {
				ThingPicker picker = am.getPlugin().getThingPickerManager().parse(thingInput);
				thing = picker.pick();
			} catch (Exception e) {
				print.error("[excellentcratesproxy(0)]: " + e.getMessage());
				return true;
			}
			//print.debug("thing: " + thingInput);
			arena.getRewardManager().addReward(targetPlayer, thing);
		}
		if (args[1].equalsIgnoreCase("giveall")) {///excellentcrates key[0] giveall[1] epic[2] 1[3] (4)
			if (args.length < 4) return true;
			String key = args[2];
			String keyCount = args[3];
			Arena arena = ma.getArenaMaster().getArenaWithName("Eternal");
			if (arena == null) return true;
			//if (!arena.isRunning()) {return true;
			setAllReadyOnMA(arena);
			if (!arena.isRunning()) {
				arena.forceStart();
			}
			for (Player p : arena.getPlayersInArena()) {
				String thingInput = String.format("cmd(Klucz: %s):/excellentcrates key give <player> %s %s", key, key, keyCount);
				Thing thing;
				try {
					ThingPicker picker = am.getPlugin().getThingPickerManager().parse(thingInput);
					thing = picker.pick();
				} catch (Exception e) {
					print.error("[excellentcratesproxy(1)]: " + e.getMessage());
					return true;
				}
				arena.getRewardManager().addReward(p, thing);
			}
		}
		return true;
	}

	public static void setAllReadyOnMA(@NotNull Arena arena) {
		for (Player p : arena.getNonreadyPlayers()) {
			if (!arena.isRunning()) {
				arena.playerReady(p);
			}
		}
	}
}
