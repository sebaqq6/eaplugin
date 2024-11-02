package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Command_adminrank implements TabExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		//Czas online, czas online AFK, komendy: kartoteka, banlist, god, fly, alts, whois, acprofil, logs, spec, survival
		//Postawione bloki, zniszczone bloki, zabite moby, zabici gracze.

		//kick, ban, warn, mute
		/*SELECT adminNick, SUM(CASE WHEN type = 0 THEN 1 ELSE 0 END) AS mute, SUM(CASE WHEN type = 1 THEN 1 ELSE 0 END) AS ban, SUM(CASE WHEN type = 2 THEN 1 ELSE 0 END) AS kick, SUM(CASE WHEN type = 3 THEN 1 ELSE 0 END) AS warn, SUM( CASE WHEN type = 0 THEN 1 ELSE 0 END + CASE WHEN type = 1 THEN 1 ELSE 0 END + CASE WHEN type = 2 THEN 1 ELSE 0 END + CASE WHEN type = 3 THEN 1 ELSE 0 END ) AS suma FROM logadmin WHERE date >= NOW() - INTERVAL 30 DAY GROUP BY adminNick ORDER BY `suma` DESC*/
		/*
		SELECT
    admin_data.adminNick,

    -- Wyniki z logserver dla komend
    COALESCE(log_data.kartoteka, 0) AS kartoteka,
    COALESCE(log_data.banlist, 0) AS banlist,
    COALESCE(log_data.god, 0) AS god,
    COALESCE(log_data.fly, 0) AS fly,
    COALESCE(log_data.alts, 0) AS alts,
    COALESCE(log_data.whois, 0) AS whois,
    COALESCE(log_data.acprofil, 0) AS acprofil,
    COALESCE(log_data.logs, 0) AS logs,
    COALESCE(log_data.spec, 0) AS spec,
    COALESCE(log_data.survival, 0) AS survival,

    -- Kolumna z liczbą wiadomości typu 2 w logserver, gdzie adminNick zawiera się w text
    COALESCE(log_data.messages, 0) AS messages,

    -- Wyniki z logadmin dla typów akcji
    COALESCE(admin_data.mute, 0) AS mute,
    COALESCE(admin_data.ban, 0) AS ban,
    COALESCE(admin_data.kick, 0) AS kick,
    COALESCE(admin_data.warn, 0) AS warn,

    -- Łączna suma działań z logadmin i komend oraz messages z logserver
    COALESCE(admin_data.suma, 0) + COALESCE(log_data.suma, 0) AS suma

FROM
    -- Sub-zapytanie dla logadmin (kary)
    (
        SELECT
            adminNick,
            SUM(CASE WHEN type = 0 THEN 1 ELSE 0 END) AS mute,
            SUM(CASE WHEN type = 1 THEN 1 ELSE 0 END) AS ban,
            SUM(CASE WHEN type = 2 THEN 1 ELSE 0 END) AS kick,
            SUM(CASE WHEN type = 3 THEN 1 ELSE 0 END) AS warn,
            SUM(
                CASE WHEN type = 0 THEN 1 ELSE 0 END +
                CASE WHEN type = 1 THEN 1 ELSE 0 END +
                CASE WHEN type = 2 THEN 1 ELSE 0 END +
                CASE WHEN type = 3 THEN 1 ELSE 0 END
            ) AS suma
        FROM
            logadmin
        WHERE
            date >= NOW() - INTERVAL 30 DAY
        GROUP BY
            adminNick
    ) AS admin_data

LEFT JOIN
    -- Sub-zapytanie dla logserver (komendy i wiadomości)
    (
        SELECT
            -- Wyodrębnienie adminNick z text na podstawie formatu logów
            SUBSTRING_INDEX(SUBSTRING_INDEX(text, ':', 1), ' ', -1) AS adminNick,
            SUM(CASE WHEN text LIKE '%kartoteka%' THEN 1 ELSE 0 END) AS kartoteka,
            SUM(CASE WHEN text LIKE '%banlist%' THEN 1 ELSE 0 END) AS banlist,
            SUM(CASE WHEN text LIKE '%god%' THEN 1 ELSE 0 END) AS god,
            SUM(CASE WHEN text LIKE '%fly%' THEN 1 ELSE 0 END) AS fly,
            SUM(CASE WHEN text LIKE '%alts%' THEN 1 ELSE 0 END) AS alts,
            SUM(CASE WHEN text LIKE '%whois%' THEN 1 ELSE 0 END) AS whois,
            SUM(CASE WHEN text LIKE '%acprofil%' THEN 1 ELSE 0 END) AS acprofil,
            SUM(CASE WHEN text LIKE '%logs%' THEN 1 ELSE 0 END) AS logs,
            SUM(CASE WHEN text LIKE '%spec%' THEN 1 ELSE 0 END) AS spec,
            SUM(CASE WHEN text LIKE '%survival%' THEN 1 ELSE 0 END) AS survival,

            -- Zliczanie wiadomości dla logserver o type = 2, gdzie text zawiera adminNick
            SUM(CASE WHEN type = 2 AND text LIKE CONCAT('%', SUBSTRING_INDEX(SUBSTRING_INDEX(text, ':', 1), ' ', -1), '%') THEN 1 ELSE 0 END) AS messages,

            -- Suma komend i messages
            SUM(
                CASE WHEN text LIKE '%kartoteka%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%banlist%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%god%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%fly%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%alts%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%whois%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%acprofil%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%logs%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%spec%' THEN 1 ELSE 0 END +
                CASE WHEN text LIKE '%survival%' THEN 1 ELSE 0 END +
                CASE WHEN type = 2 AND text LIKE CONCAT('%', SUBSTRING_INDEX(SUBSTRING_INDEX(text, ':', 1), ' ', -1), '%') THEN 1 ELSE 0 END -- dodanie messages do sumy
            ) AS suma
        FROM
            logserver
        WHERE
            date >= NOW() - INTERVAL 30 DAY
        GROUP BY
            adminNick
    ) AS log_data

ON admin_data.adminNick = log_data.adminNick
ORDER BY
    suma DESC;*/
		commandSender.sendMessage("adminrank here");
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		return List.of();
	}
}
