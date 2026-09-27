package pl.kiosel.rosacore;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RosaLogger {

	private static final RosaLogger INSTANCE = new RosaLogger();
	private Logger delegate = Logger.getLogger(RosaPlugin.getCoreName());
	private final String prefix;

	RosaLogger() {
		this.prefix = "[" + RosaPlugin.getCoreName() + "] ";
	}

	void setPlugin(RosaPlugin plugin) {
		Objects.requireNonNull(plugin, "plugin");
		this.delegate = plugin.getLogger();
	}

	public void info(String message) {
		log(Level.INFO, message);
	}

	public void warning(String message) {
		log(Level.WARNING, message);
	}

	public void severe(String message) {
		log(Level.SEVERE, message);
	}

	public void log(Level level, String message) {
		this.delegate.log(Objects.requireNonNull(level, "level"), this.prefix + message);
	}

	public void log(Level level, String message, Throwable throwable) {
		this.delegate.log(level, this.prefix + message, throwable);
	}

	public static RosaLogger getInstance() {
		return INSTANCE;
	}
}
