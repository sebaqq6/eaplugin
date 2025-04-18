package pl.eadventure.plugin.FunEvents;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FunEventsCommands {
	private static final String mainCmd = "/fe";
	public static FunEventsManager fem = gVar.funEventsManager;
	public static int recordsTime = 60 * 5;

	public static void onFunEventCommand(CommandSender sender, String[] args) {//need permission eadventureplugin.cmd.fe

		if (args.length == 0) {
			Utils.commandUsageMessage(sender, String.format("/%s [komenda] [opcjonalnie: nazwa eventu]", mainCmd));
			return;
		}
		String command = args[0];
		switch (command) {
			case "start" -> startCommand(sender, args);
			case "stopzapisy" -> stopCommand(sender, args);
			case "forcefinish" -> forcefinishCommand(sender, args);
			case "world" -> worldCommand(sender, args);
			case "tp" -> tpCommand(sender, args);
			case "recordsduration" -> recordsdurationCommand(sender, args);
			case "reload" -> reloadCommand(sender, args);
		}
	}

	//-------------------------------------Commands--------------------------------------
	//next args >= 1
	//
	//
	/*                                 /fe start                          */
	public static void startCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			Utils.commandUsageMessage(sender, "/fe start [event]");
			return;
		}
		String eventName = args[1];
		if (fem.startRecord(eventName, recordsTime)) {
			sender.sendMessage(Utils.mm("<#00FF00>Otworzyłeś/aś zapisy na: <grey><bold>" + fem.getEvent(eventName).getEventName()));
		} else {
			sender.sendMessage(Utils.mm("<#FF0000>Błąd: Albo już trwają zapisy, albo zabawa już trwa."));
		}
	}

	/*                                 /fe stopzapisy                          */
	public static void stopCommand(CommandSender sender, String[] args) {
		FunEvent fe = fem.getActualFunEvent();
		if (fem.stopRecord()) {
			fe.setStatus(FunEvent.Status.FREE);
			sender.sendMessage(Utils.mm("<#FF0000><bold>Anulowano</bold> aktualnie trwające zapisy."));
		} else {
			sender.sendMessage(Utils.mm("<grey>W tym momencie nie trwają żadne zapisy."));
		}
	}

	/*                                 /fe forcefinish                          */
	public static void forcefinishCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			Utils.commandUsageMessage(sender, "/fe forcefinish [event]");
			return;
		}
		String eventName = args[1];
		if (!FunEventsManager.getEventKeysAsList().contains(eventName)) {
			sender.sendMessage(Utils.mm("<grey>Taki event nie istnieje."));
			return;
		}
		if (fem.getEvent(eventName).finishEvent()) {
			sender.sendMessage(Utils.mm("<#FF0000><bold>Wymuszono</bold> zakończenie eventu: " + fem.getEvent(eventName).getEventName()));
		} else {
			sender.sendMessage(Utils.mm("<grey>Ten event obecnie nie trwa."));
		}
	}

	/*                                 /fe world                          */
	public static void worldCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			player.teleport(new Location(Bukkit.getWorld("world_utility"), -1, 65, 0));
		}
	}

	/*                                 /fe tp                          */
	public static void tpCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length < 2) {
				Utils.commandUsageMessage(sender, "/fe tp [event]");
				return;
			}
			String eventName = args[1];
			if (!FunEventsManager.getEventKeysAsList().contains(eventName)) {
				sender.sendMessage(Utils.mm("<grey>Taki event nie istnieje."));
				return;
			}
			player.teleport(fem.getEvent(eventName).getArenaPos());
		}
	}

	/*                                 /fe recordsduration                          */
	public static void recordsdurationCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			Utils.commandUsageMessage(sender, "/fe recordsduration [ilość sekund]");
			return;
		}
		String durationStr = args[1];
		int duration = Integer.parseInt(durationStr);
		recordsTime = duration;
		sender.sendMessage(Utils.mm("<#FF0000>Ustawiono czas zapisów na: " + recordsTime + " sekund."));
	}

	/*                                 /fe reload                          */
	public static void reloadCommand(CommandSender sender, String[] args) {
		fem.registerEvents();
		sender.sendMessage(Utils.mm("<#FF0000>Przeładowano eventy."));
	}

	//-------------------------------------TAB COMPLETE--------------------------------------
	//next args >= 1
	public static List<String> cmdlist = Arrays.asList("start", "stopzapisy", "forcefinish", "recordsduration", "world", "reload", "tp");

	public static List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1)//args[0] - main commad
		{
			return StringUtil.copyPartialMatches(args[0], cmdlist, new ArrayList<>());
		} else if (args.length == 2) {// args[1] - eventname
			List<String> subcommands = Arrays.asList("start", "forcefinish", "tp");
			if (subcommands.contains(args[0].toLowerCase())) {
				return StringUtil.copyPartialMatches(args[1], FunEventsManager.getEventKeysAsList(), new ArrayList<>());
			}
		}
		return Collections.emptyList();
	}
}
