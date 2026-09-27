package pl.kiosel.rosacore.input;

import pl.kiosel.rosacore.RosaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class Debug {

	private final RosaPlugin plugin;
	private FileWriter writer;

	public Debug(RosaPlugin plugin) {
		this.plugin = plugin;
	}

	public void setup() {
		if (!plugin.isDev()) return;

		File debugLogFile = new File(plugin.getDataFolder(), "debug.txt");

		try {
			if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
				throw new IOException("Could not create plugin data folder " + plugin.getDataFolder());
			}
			if (!debugLogFile.exists()) {
				if (debugLogFile.createNewFile()) {
					plugin.getLogger().info("Created file: " + debugLogFile);
				}
			}
			new PrintWriter(debugLogFile).close();

			writer = new FileWriter(debugLogFile, true);
			debug("Setup debug complete");
		} catch (IOException e) {
			plugin.getLogger().info("Failed to instantiate FileWriter");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
		}
	}

	public synchronized void close() {
		if (writer == null) return;

		try {
			if (plugin.isDev()) debug("Closing debug");
			writer.close();
			writer = null;
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to close FileWriter");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
		}
	}

	public synchronized void debug(String message) {
		if (writer == null) return;
		if (!plugin.isDev()) return;

		try {
			plugin.getLogger().info(message);
			Calendar c = Calendar.getInstance();
			String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(c.getTime());
			writer.write(String.format("[%s] %s\r\n", timestamp, message));
			writer.flush();
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to print debug message.");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
		}
	}

	public synchronized void error(String message) {
		if (writer == null) return;
		if (!plugin.isDev()) return;

		try {
			plugin.getLogger().warning(message);
			Calendar c = Calendar.getInstance();
			String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(c.getTime());
			writer.write(String.format("[%s] %s\r\n", timestamp, "WARNING:" + message));
			writer.flush();
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to print debug message.");
			plugin.getLogger().info(Arrays.toString(e.getStackTrace()));
		}
	}

	public synchronized void debug(Throwable throwable) {
		if (writer == null) return;
		if (throwable == null) return;
		if (!plugin.isDev()) return;

		PrintWriter pw = new PrintWriter(writer);
		throwable.printStackTrace(pw);
		pw.flush();
	}

	public void debug(String message, Throwable throwable) {
		debug(message);
		debug(throwable);
	}
}
