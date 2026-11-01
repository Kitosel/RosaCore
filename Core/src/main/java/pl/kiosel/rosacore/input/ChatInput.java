package pl.kiosel.rosacore.input;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.RosaMessage;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInput {

	private static final Map<UUID, ChatInput> ACTIVE_INPUTS = new ConcurrentHashMap<>();
	private static boolean listenerRegistered = false;

	private final RosaPlugin plugin;
	private final Player player;
	private final OnConfirmChat onConfirmChat;

	private RosaMessage timeOutMessage;
	private OnCloseChat onCloseChat = null;
	private OnCancelInput onCancelInput = null;
	private int taskId = -1;

	private ChatInput(RosaPlugin plugin, Player player, OnConfirmChat onConfirmChat) {
		this.plugin = plugin;
		this.player = player;
		this.onConfirmChat = onConfirmChat;
	}

	public static void registerListener(RosaPlugin plugin) {
		if (listenerRegistered) return;
		Bukkit.getPluginManager().registerEvents(new ChatInputListener(plugin), plugin);
		listenerRegistered = true;
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, OnConfirmChat handler) {
		return showPrompt(plugin, player, (String) null, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, String message, OnConfirmChat handler) {
		RosaMessage rosaMessage = message != null ? plugin.getMessages().newMessage(message) : null;
		return showPrompt(plugin, player, rosaMessage, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, RosaMessage message, OnConfirmChat handler) {
		return showPrompt(plugin, player, message, false, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, RosaMessage message, boolean prefixedMessage, OnConfirmChat handler) {
		Objects.requireNonNull(plugin, "plugin cannot be null");
		Objects.requireNonNull(player, "player cannot be null");
		Objects.requireNonNull(handler, "onConfirmChat cannot be null");

		ChatInput oldPrompt = ACTIVE_INPUTS.remove(player.getUniqueId());
		if (oldPrompt != null) {
			oldPrompt.cancelTimeoutTask();
		}

		ChatInput prompt = new ChatInput(plugin, player, handler);
		ACTIVE_INPUTS.put(player.getUniqueId(), prompt);

		player.closeInventory();

		if (message != null) {
			if (prefixedMessage) {
				message.sendPrefixed(player);
			} else {
				message.send(player);
			}
		}
		return prompt;
	}

	public static boolean isRegistered(Player player) {
		return ACTIVE_INPUTS.containsKey(player.getUniqueId());
	}

	public ChatInput onClose(OnCloseChat onCloseChat) {
		this.onCloseChat = onCloseChat;
		return this;
	}

	public ChatInput onCancel(OnCancelInput onCancelInput) {
		this.onCancelInput = onCancelInput;
		return this;
	}

	public ChatInput setTimeOutMessage(RosaMessage timeOutMessage) {
		this.timeOutMessage = timeOutMessage;
		return this;
	}

	public ChatInput setTimeOutMessage(String timeOutMessage) {
		this.timeOutMessage = plugin.getMessages().newMessage(timeOutMessage);
		return this;
	}

	public ChatInput setTimeout(long ticks) {
		return setTimeout(ticks, false);
	}

	public ChatInput setTimeout(long ticks, boolean prefixedMessage) {
		cancelTimeoutTask();

		this.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
			if (ACTIVE_INPUTS.remove(player.getUniqueId()) == null) {
				return;
			}

			if (this.onCloseChat != null) {
				this.onCloseChat.onClose();
			}

			if (timeOutMessage == null) {
				timeOutMessage = plugin.getMessages().newMessage("&cYour action has timed out.");
			}

			if (prefixedMessage) {
				timeOutMessage.sendPrefixed(player);
			} else {
				timeOutMessage.send(player);
			}
		}, ticks);

		return this;
	}

	private void cancelTimeoutTask() {
		if (this.taskId != -1) {
			Bukkit.getScheduler().cancelTask(this.taskId);
			this.taskId = -1;
		}
	}

	public interface OnConfirmChat {
		void onChat(ChatConfirmEvent event);
	}

	public interface OnCloseChat {
		void onClose();
	}

	public interface OnCancelInput {
		void onCancel();
	}

	@Getter
	public static class ChatConfirmEvent {
		private final Player player;
		private final String message;

		public ChatConfirmEvent(Player player, String message) {
			this.player = player;
			this.message = message;
		}
	}

	private static class ChatInputListener implements Listener {
		private final RosaPlugin plugin;

		public ChatInputListener(RosaPlugin plugin) {
			this.plugin = plugin;
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onChat(AsyncPlayerChatEvent event) {
			Player player = event.getPlayer();
			ChatInput prompt = ACTIVE_INPUTS.remove(player.getUniqueId());
			if (prompt == null) return;

			event.setCancelled(true);
			prompt.cancelTimeoutTask();

			player.sendMessage(ColorUtils.tl("&8» &r" + event.getMessage()));

			ChatConfirmEvent chatConfirmEvent = new ChatConfirmEvent(player, event.getMessage());
			try {
				prompt.onConfirmChat.onChat(chatConfirmEvent);
			} catch (Throwable t) {
				plugin.getDebug().debug("Failed to process chat input prompt", t);
			}

			if (prompt.onCloseChat != null) {
				plugin.getRosaScheduler().runGlobalLater(() -> prompt.onCloseChat.onClose(), 0L);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onCancel(PlayerCommandPreprocessEvent event) {
			Player player = event.getPlayer();
			if (!event.getMessage().toLowerCase().startsWith("/cancel")) return;

			ChatInput prompt = ACTIVE_INPUTS.remove(player.getUniqueId());
			if (prompt == null) return;

			event.setCancelled(true);
			prompt.cancelTimeoutTask();

			if (prompt.onCancelInput != null) {
				plugin.getRosaScheduler().runGlobalLater(() -> prompt.onCancelInput.onCancel(), 0L);
			} else if (prompt.onCloseChat != null) {
				plugin.getRosaScheduler().runGlobalLater(() -> prompt.onCloseChat.onClose(), 0L);
			}
		}

		@EventHandler
		public void onQuit(PlayerQuitEvent event) {
			ChatInput prompt = ACTIVE_INPUTS.remove(event.getPlayer().getUniqueId());
			if (prompt != null) {
				prompt.cancelTimeoutTask();
			}
		}
	}
}