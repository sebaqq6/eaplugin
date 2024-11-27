package pl.eadventure.plugin;

import org.bukkit.inventory.ItemStack;
import pl.eadventure.plugin.FunEvents.FunEventsManager;
import pl.eadventure.plugin.Modules.AnnounceManager;
import pl.eadventure.plugin.Modules.LiveStream;
import pl.eadventure.plugin.Modules.Top.TopBreakBlocks;
import pl.eadventure.plugin.Modules.Top.TopDonate;
import pl.eadventure.plugin.Modules.Top.TopGearScore;
import pl.eadventure.plugin.Utils.ChatInputCapture;
import pl.eadventure.plugin.Utils.ColorIssueResolverIA;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.UUID;

public class gVar {
	public static HashMap<InetAddress, Boolean> isBanned = new HashMap<>();
	//MySQL login data
	public static String mysqlHost;
	public static int mysqlPort = 3306;
	public static String mysqlDatabase;
	public static String mysqlUser;
	public static String mysqlPassword;
	//rozsypanka
	public static String scatteredResult = null;
	public static double scatteredBounty = 0.0;

	//
	public static HashMap<String, ItemStack> customItems = new HashMap<>();
	public static ColorIssueResolverIA colorIssueResolverIA;
	public static FunEventsManager funEventsManager;
	public static TopBreakBlocks topBreakBlocks;
	public static TopGearScore topGearScore;
	public static TopDonate topDonate;
	public static AnnounceManager announceManager;
	public static LiveStream liveStream;
	public static boolean antiBot = true;
	public static ChatInputCapture chatInputCapture;
	public static HashMap<UUID, Boolean> hiddenTabName = new HashMap<>();
}
