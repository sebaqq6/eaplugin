package pl.eadventure.plugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PlayerData {
	//head
	private Player player = null;
    private static HashMap<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
	//temp data
	public boolean eventAnnChat = false;//player in Annuance chat mode
	public boolean decayDebug = false;
	//player temp data
	public boolean deathOnPVPArena = false;//player last death on arena pvp
	public ItemStack[] itemsWhileDeath = null;//items while death player
	public ItemStack[] armorWhileDeath = null;//armor while death
	public int itemsCountWhileDeath = 0;//count items while death
	public int armorCountWhileDeath = 0;//count armor while death
	public int sessionOnlineSeconds = 0;
	public PunishmentSystem.WarnData warnData = null;
	public HomesInterface homesInterface = null;
	//MySQL data
	public int dbid = 0;//database ID
	public int registerDate = 0;
	public boolean immunity = false; //
	public int onlineHours = 0, onlineMinutes = 0, onlineSeconds = 0;
	public int maxSessionOnlineSeconds = 0;
	public int mutedExpire = 0;
	public String mutedBy = " ";
	public String mutedReason = " ";
	public String clientBrand = "null";
	//end variables
	public PlayerData(Player player)
	{
		if(player == null) return;
		this.player = player;
		players.put(player.getUniqueId(), this);
		print.debug("Gracz: " + player.getName() + " - stworzono instancje danych!");
		loadDataFromMySQL(player);
	}
    public static PlayerData get(Player player) {
        if(players.containsKey(player.getUniqueId())) return players.get(player.getUniqueId());
        else return new PlayerData(player);
    }
    public static void free(Player player)
    {
    	if(players.containsKey(player.getUniqueId())) players.remove(player.getUniqueId());
    	else print.error("Gracz: " + player.getName() + " - Brak instancji danych! Coś jest nie tak!");
    }
    public void loadDataFromMySQL(Player player) {
		//load WarnData
		warnData = new PunishmentSystem.WarnData();
		warnData.load(player.getUniqueId());

        MySQLStorage storage = EternalAdventurePlugin.getMySQL();
        if (storage.isConnect()) {
            UUID uuid = player.getUniqueId();
            String sql = "SELECT * FROM `players` WHERE `uuid`='" + uuid + "';";
           
            storage.query(sql, queryResult -> {
                if (queryResult != null) {
                    int numRows = (int) queryResult.get("num_rows");
                    //ArrayList<HashMap<Object, Object>> rows = (ArrayList<HashMap<Object, Object>>) queryResult.get("rows");
                    if (numRows >= 1) {
                        handleExistingPlayer(queryResult);
                    } else {
                    	registerNewPlayer(storage);
                    }
                } else {
                    print.error("PlayerData->loadDataFromMySQL - błąd zapytania:");
                    print.error(sql);
                }
            });
        } else {
            print.error("PlayerData->loadDataFromMySQL - nie udało się ustanowić połączenia!");
        }
    }
    //Player Exist - Load data...
    private void handleExistingPlayer(HashMap<Object, Object> queryResult) {
		MySQLStorage storage = EternalAdventurePlugin.getMySQL();
        HashMap<?, ?> row = (HashMap<?, ?>) queryResult.get("row");

        dbid = (int) row.get("id");
        print.debug("Gracz: " + player.getName() + " - posiada konto EAP(ID: " + dbid + ").");
        //Load data start
		registerDate = (int) row.get("registerdate");
		immunity = ((int) row.get("immunity") == 1);
		onlineHours = (int) row.get("onlineHours");
		onlineMinutes = (int) row.get("onlineMinutes");
		onlineSeconds = (int) row.get("onlineSeconds");
		maxSessionOnlineSeconds = (int) row.get("maxSessionOnlineSeconds");
		mutedExpire = (int) row.get("mutedExpire");
		mutedBy = (String) row.get("mutedBy");
		mutedReason = (String) row.get("mutedReason");
		//Load data end
		//Update some information
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(player.getAddress().getAddress().getHostAddress());
		parameters.add(player.getName());
		parameters.add(dbid);
		storage.executeSafe("UPDATE players SET ip=?, nick=? WHERE id=?;", parameters);
	}
    //Register
    private void registerNewPlayer(MySQLStorage storage) {
        print.debug("Gracz: " + player.getName() + " - nie posiada konta EAP...");

		//Add to punish system
		PunishmentSystem.getListPlayersCanBeBanned().add(player.getName());;
		PunishmentSystem.getListPlayersAll().add(player.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                //dbid = storage.executeGetInsertID("INSERT INTO `players`(`nick`, `uuid`, `registerdate`, `ip`) VALUES ('" + player.getName() + "', '" + uuid + "' , '" + Utils.getUnixTimestamp() + "', '" + player.getAddress().getAddress() + "');");
                ArrayList<Object> parameters = new ArrayList<>();
				parameters.add(player.getName());
				parameters.add(player.getUniqueId().toString());
				parameters.add(Utils.getUnixTimestamp());
				parameters.add(player.getAddress().getAddress().getHostAddress());
				dbid = storage.executeGetInsertID("INSERT INTO `players`(`nick`, `uuid`, `registerdate`, `ip`) VALUES (?, ?, ?, ?);", parameters);
                print.debug("Gracz: " + player.getName() + " - zarejestrowano konto EAP o ID: " + dbid + ".");
            }
        }.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
    }
}
