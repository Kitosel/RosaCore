package pl.kiosel.rosacore.input;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.RosaMessage;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatInput implements Listener {

	private static final List<UUID> REGISTERED = new ArrayList<>();

	private final RosaPlugin plugin;

	private RosaMessage timeOutMessage;

	private final onConfirmChat onConfirmChat;
	private onCloseChat onCloseChat = null;
	private onCancelInput onCancelInput = null;

	private int taskId;

	private Listener listener;

	private ChatInput(RosaPlugin plugin, Player player, onConfirmChat onConfirmChat) {
		this.plugin = plugin;
		this.onConfirmChat = onConfirmChat;

		REGISTERED.add(player.getUniqueId());
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, onConfirmChat handler) {
		return showPrompt(plugin, player, (String) null, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, String message, onConfirmChat handler) {
		RosaMessage rosaMessage = null;
		if (message != null) {
			rosaMessage = plugin.getMessages().newMessage(message);
		}
		return showPrompt(plugin, player, rosaMessage, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, RosaMessage message, onConfirmChat handler) {
		return showPrompt(plugin, player, message, false, handler);
	}

	public static ChatInput showPrompt(RosaPlugin plugin, Player player, RosaMessage message, boolean prefixedMessage, onConfirmChat handler) {
		Objects.requireNonNull(plugin, "plugin cannot be null");
		Objects.requireNonNull(player, "player cannot be null");
		Objects.requireNonNull(handler, "onConfirmChat cannot be null");

		ChatInput prompt = new ChatInput(plugin, player, handler);
		prompt.startInput(plugin);
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
		return REGISTERED.contains(player.getUniqueId());
	}

	public static boolean unregister(Player player) {
		return REGISTERED.remove(player.getUniqueId());
	}

	public ChatInput onClose(onCloseChat onCloseChat) {
		this.onCloseChat = onCloseChat;
		return this;
	}

	public ChatInput onCancel(onCancelInput onCancelInput) {
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

	public ChatInput setTimeout(Player player, long ticks) {
		return setTimeout(player, ticks, false);
	}

	public ChatInput setTimeout(Player player, long ticks, boolean prefixedMessage) {
		this.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
			if (this.onCloseChat != null) {
				this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () ->
						this.onCloseChat.onClose(), 0L);
			}

			HandlerList.unregisterAll(this.listener);
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

	private void startInput(RosaPlugin plugin) {
		this.listener = new Listener() {
			@EventHandler(priority = EventPriority.LOWEST)
			public void onChat(AsyncPlayerChatEvent event) {
				Player player = event.getPlayer();

				if (!ChatInput.isRegistered(player)) {
					return;
				}

				ChatInput.unregister(player);
				event.setCancelled(true);

				ChatConfirmEvent chatConfirmEvent = new ChatConfirmEvent(player, event.getMessage());

				player.sendMessage(ColorUtils.tl("&8» &r" + event.getMessage()));

				try {
					ChatInput.this.onConfirmChat.onChat(chatConfirmEvent);
				} catch (Throwable t) {
					plugin.getDebug().debug("Failed to process chat input prompt", t);
				}

				if (ChatInput.this.onCloseChat != null) {
					plugin.getRosaScheduler().runGlobalLater(() -> ChatInput.this.onCloseChat.onClose(), 0L);
				}

				HandlerList.unregisterAll(ChatInput.this.listener);
				Bukkit.getScheduler().cancelTask(ChatInput.this.taskId);
			}

			@EventHandler(priority = EventPriority.LOWEST)
			public void onCancel(PlayerCommandPreprocessEvent event) {
				Player player = event.getPlayer();

				if (!ChatInput.isRegistered(player)) {
					return;
				}

				ChatInput.unregister(player);

				if (event.getMessage().toLowerCase().startsWith("/cancel")) {
					event.setCancelled(true);
				}

				if (ChatInput.this.onCancelInput != null) {
					plugin.getRosaScheduler().runGlobalLater(() -> ChatInput.this.onCancelInput.onCancel(), 0L);
				} else if (ChatInput.this.onCloseChat != null) {
					plugin.getRosaScheduler().runGlobalLater(() -> ChatInput.this.onCloseChat.onClose(), 0L);
				}

				HandlerList.unregisterAll(ChatInput.this.listener);
				Bukkit.getScheduler().cancelTask(ChatInput.this.taskId);
			}
		};

		Bukkit.getPluginManager().registerEvents(this.listener, plugin);
	}

	public interface onConfirmChat {
		void onChat(ChatConfirmEvent event);
	}

	public interface onCloseChat {
		void onClose();
	}

	public interface onCancelInput {
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
}
