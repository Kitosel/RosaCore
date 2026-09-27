package pl.kiosel.core.input;

import pl.kiosel.core.MetaPlugin;
import pl.kiosel.core.chat.AdventureUtils;
import pl.kiosel.core.locale.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class Debug {

    private final MetaPlugin plugin;
    private FileWriter fw;

    protected static String debug = AdventureUtils.h + Message.p;

    public Debug(MetaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
		if(!plugin.isDev()) return;

        File debugLogFile = new File(plugin.getDataFolder(), "debug.txt");

        try {
            if (!debugLogFile.exists()) {
                if (debugLogFile.createNewFile()) {
					plugin.getLogger().info("Created file: " + debugLogFile);
				}
            }
            new PrintWriter(debugLogFile).close();

            fw = new FileWriter(debugLogFile, true);
            debug("Setup debug complete");
        } catch (IOException e) {
            plugin.getLogger().info("Failed to instantiate FileWriter");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
        }
    }

    public void close() {
        if(fw == null) return;
		if(!plugin.isDev()) return;

        try {
            debug("Closing debug");
            fw.close();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to close FileWriter");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
        }
    }

    public void debug(String message) {
        if(fw == null) return;
		if(!plugin.isDev()) return;

        try {
            plugin.getLogger().severe(message);
            Calendar c = Calendar.getInstance();
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(c.getTime());
            fw.write(String.format("[%s] %s\r\n", timestamp, message));
            fw.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to print debug message.");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
        }
    }

    public void error(String message) {
        if(fw == null) return;
        if(!plugin.isDev()) return;

        try {
            plugin.getLogger().warning(message);
            Calendar c = Calendar.getInstance();
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(c.getTime());
            fw.write(String.format("[%s] %s\r\n", timestamp, "WARNING:" +message));
            fw.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to print debug message.");
            plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
        }
    }

    public void debug(Throwable throwable) {
        if(fw == null) return;
        if(throwable == null) return;
		if(!plugin.isDev()) return;

        PrintWriter pw = new PrintWriter(fw);
        throwable.printStackTrace(pw);
        pw.flush();
    }

    public void debug(String message, Throwable throwable) {
        debug(message);
        debug(throwable);
    }
}
