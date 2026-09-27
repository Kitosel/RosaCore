package pl.kiosel.rosacore.message;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.scheduler.RosaScheduler;
import pl.kiosel.rosacore.scheduler.RosaTask;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RosaMessenger implements AutoCloseable {

	private static final Pattern PLACEHOLDER = Pattern.compile("%([A-Za-z0-9_.-]+)%");
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	public static final int DEFAULT_FADE_IN_TICKS = 10;
	public static final int DEFAULT_STAY_TICKS = 70;
	public static final int DEFAULT_FADE_OUT_TICKS = 20;
	public static final int DEFAULT_ANIMATION_TICKS_PER_CHARACTER = 2;

	private final BukkitAudiences audiences;
	private final LegacyComponentSerializer serializer;
	private final LegacyColorizer colorizer;
	private final RosaScheduler scheduler;
	private final Map<UUID, TitleAnimation> titleAnimations = new ConcurrentHashMap<>();
	@Getter
	private boolean closed;

	public RosaMessenger(RosaPlugin plugin, MinecraftVersion minecraftVersion) {
		RosaPlugin checkedPlugin = Objects.requireNonNull(plugin, "plugin");
		this.audiences = BukkitAudiences.create(checkedPlugin);
		this.serializer = LegacyComponentSerializer.legacySection();
		this.colorizer = LegacyColorizer.forVersion(
				Objects.requireNonNull(minecraftVersion, "minecraftVersion"));
		this.scheduler = checkedPlugin.getRosaScheduler();
	}

	public void send(CommandSender sender, String message, Object... placeholders) {
		requireOpen();
		this.audiences.sender(Objects.requireNonNull(sender, "sender"))
				.sendMessage(component(message, placeholders));
	}

	public void sendComponent(CommandSender sender, Component message) {
		requireOpen();
		this.audiences.sender(Objects.requireNonNull(sender, "sender"))
				.sendMessage(Objects.requireNonNull(message, "message"));
	}

	public void actionBar(Player player, String message, Object... placeholders) {
		playerAudience(player).sendActionBar(component(message, placeholders));
	}

	public void title(Player player, String title, String subtitle) {
		title(player, title, subtitle,
				DEFAULT_FADE_IN_TICKS, DEFAULT_STAY_TICKS, DEFAULT_FADE_OUT_TICKS);
	}

	public void title(Player player, String title, String subtitle, Object... placeholders) {
		title(player, title, subtitle,
				DEFAULT_FADE_IN_TICKS, DEFAULT_STAY_TICKS, DEFAULT_FADE_OUT_TICKS,
				placeholders);
	}

	public void title(Player player, String title, String subtitle,
					  int fadeInTicks, int stayTicks, int fadeOutTicks) {
		title(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks, new Object[0]);
	}

	public void title(Player player, String title, String subtitle,
					  int fadeInTicks, int stayTicks, int fadeOutTicks,
					  Object... placeholders) {
		requireOpen();
		stopTitleAnimation(player);
		showTitle(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks, placeholders);
	}

	public void animatedTitle(Player player, String title, String subtitle) {
		animatedTitle(player, title, subtitle, DEFAULT_ANIMATION_TICKS_PER_CHARACTER);
	}

	public void animatedTitle(Player player, String title, String subtitle,
							  int ticksPerCharacter, Object... placeholders) {
		requireOpen();
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		if (ticksPerCharacter < 1) {
			throw new IllegalArgumentException("Animation speed must be at least 1 tick per character");
		}

		MessagePlaceholders values = MessagePlaceholders.pairs(
				placeholders == null ? new Object[0] : placeholders);
		String renderedTitle = renderText(title, values);
		String renderedSubtitle = renderText(subtitle, values);
		TitleAnimation animation = new TitleAnimation(
				checkedPlayer,
				renderedTitle,
				renderedSubtitle,
				ticksPerCharacter
		);

		UUID playerId = checkedPlayer.getUniqueId();
		TitleAnimation previous = this.titleAnimations.put(playerId, animation);
		if (previous != null) previous.cancel();

		if (animation.isEmpty()) {
			animation.complete();
			return;
		}

		RosaTask task = this.scheduler.runForEntityTimer(
				checkedPlayer,
				animation,
				animation::cancel,
				0L,
				ticksPerCharacter
		);
		animation.bind(task);
	}

	public void animatedSubtitle(Player player, String subtitle) {
		animatedSubtitle(player, subtitle, DEFAULT_ANIMATION_TICKS_PER_CHARACTER);
	}

	public void animatedSubtitle(Player player, String subtitle,
								 int ticksPerCharacter, Object... placeholders) {
		animatedTitle(player, "", subtitle, ticksPerCharacter, placeholders);
	}

	public void stopTitleAnimation(Player player) {
		if (player == null) return;
		TitleAnimation animation = this.titleAnimations.remove(player.getUniqueId());
		if (animation != null) animation.cancel();
	}

	private void showTitle(Player player, String title, String subtitle,
						   int fadeInTicks, int stayTicks, int fadeOutTicks,
						   Object... placeholders) {
		Title.Times times = Title.Times.times(
				durationFromTicks(fadeInTicks),
				durationFromTicks(stayTicks),
				durationFromTicks(fadeOutTicks)
		);
		Title rendered = Title.title(
				component(title, placeholders),
				component(subtitle, placeholders),
				times
		);
		playerAudience(player).showTitle(rendered);
	}

	public void subtitle(Player player, String subtitle) {
		subtitle(player, subtitle,
				DEFAULT_FADE_IN_TICKS, DEFAULT_STAY_TICKS, DEFAULT_FADE_OUT_TICKS);
	}

	public void subtitle(Player player, String subtitle, Object... placeholders) {
		subtitle(player, subtitle,
				DEFAULT_FADE_IN_TICKS, DEFAULT_STAY_TICKS, DEFAULT_FADE_OUT_TICKS,
				placeholders);
	}

	public void subtitle(Player player, String subtitle,
						 int fadeInTicks, int stayTicks, int fadeOutTicks) {
		subtitle(player, subtitle, fadeInTicks, stayTicks, fadeOutTicks, new Object[0]);
	}

	public void subtitle(Player player, String subtitle,
						 int fadeInTicks, int stayTicks, int fadeOutTicks,
						 Object... placeholders) {
		title(player, "", subtitle, fadeInTicks, stayTicks, fadeOutTicks, placeholders);
	}

	public void gradientActionBar(Player player, String message,
								  String firstColor, String secondColor,
								  Object... placeholders) {
		playerAudience(player).sendActionBar(
				gradientComponent(message, firstColor, secondColor, placeholders));
	}

	public void gradient(Player player, String message, String firstColor, String secondColor) {
		gradientActionBar(player, message, firstColor, secondColor);
	}

	public void clearTitle(Player player) {
		stopTitleAnimation(player);
		playerAudience(player).clearTitle();
	}

	public void resetTitle(Player player) {
		stopTitleAnimation(player);
		playerAudience(player).resetTitle();
	}

	@Override
	public void close() {
		if (this.closed) return;
		this.closed = true;
		for (TitleAnimation animation : this.titleAnimations.values()) animation.cancel();
		this.titleAnimations.clear();
		this.audiences.close();
	}

	public Component component(String message, Object... nameValuePairs) {
		MessagePlaceholders placeholders = MessagePlaceholders.pairs(
				nameValuePairs == null ? new Object[0] : nameValuePairs);
		String rendered = renderText(message, placeholders);
		return this.serializer.deserialize(this.colorizer.colorize(rendered));
	}

	Component gradientComponent(String message, String firstColor, String secondColor,
								Object... nameValuePairs) {
		return applyGradient(component(message, nameValuePairs), firstColor, secondColor);
	}

	static Component applyGradient(Component content, String firstColor, String secondColor) {
		String format = "<gradient:" + gradientColor(firstColor) + ":"
				+ gradientColor(secondColor) + "><rosa_content></gradient>";
		return MINI_MESSAGE.deserialize(format,
				Placeholder.component("rosa_content", Objects.requireNonNull(content, "content")));
	}

	Audience playerAudience(Player player) {
		requireOpen();
		return this.audiences.player(Objects.requireNonNull(player, "player"));
	}

	private static String render(String message, MessagePlaceholders placeholders) {
		Matcher matcher = PLACEHOLDER.matcher(message);
		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			String name = matcher.group(1);
			String replacement = placeholders.contains(name)
					? placeholders.get(name)
					: matcher.group(0);
			matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(output);
		return output.toString();
	}

	private static String renderText(String message, MessagePlaceholders placeholders) {
		return render(Objects.requireNonNull(message, "message").replace("\\n", "\n"), placeholders);
	}

	private static List<String> animationFrames(String text) {
		if (text.isEmpty()) return Collections.emptyList();
		List<String> frames = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int index = 0;
		while (index < text.length()) {
			int formattingLength = formattingCodeLength(text, index);
			if (formattingLength > 0) {
				current.append(text, index, index + formattingLength);
				index += formattingLength;
				continue;
			}

			int codePoint = text.codePointAt(index);
			current.appendCodePoint(codePoint);
			index += Character.charCount(codePoint);
			frames.add(current.toString());
		}
		return frames;
	}

	private static int formattingCodeLength(String text, int index) {
		char marker = text.charAt(index);
		if (marker != '&' && marker != '\u00a7') return 0;
		if (index + 1 >= text.length()) return 0;

		char code = text.charAt(index + 1);
		if (marker == '&' && code == '#' && hasHexDigits(text, index + 2, 6)) return 8;
		if (marker == '\u00a7' && (code == 'x' || code == 'X')
				&& hasSectionHexDigits(text, index + 2)) return 14;
		return isLegacyCode(code) ? 2 : 0;
	}

	private static boolean hasHexDigits(String text, int start, int length) {
		if (start + length > text.length()) return false;
		for (int index = start; index < start + length; index++) {
			if (Character.digit(text.charAt(index), 16) < 0) return false;
		}
		return true;
	}

	private static boolean hasSectionHexDigits(String text, int start) {
		if (start + 12 > text.length()) return false;
		for (int index = start; index < start + 12; index += 2) {
			if (text.charAt(index) != '\u00a7'
					|| Character.digit(text.charAt(index + 1), 16) < 0) return false;
		}
		return true;
	}

	private static boolean isLegacyCode(char code) {
		char normalized = Character.toLowerCase(code);
		return normalized >= '0' && normalized <= '9'
				|| normalized >= 'a' && normalized <= 'f'
				|| normalized >= 'k' && normalized <= 'o'
				|| normalized == 'r';
	}

	static Duration durationFromTicks(int ticks) {
		if (ticks < 0) {
			throw new IllegalArgumentException("Title duration cannot be negative: " + ticks);
		}
		return Duration.ofMillis(Math.multiplyExact((long) ticks, 50L));
	}

	static String gradientColor(String color) {
		String value = Objects.requireNonNull(color, "color").trim().toLowerCase(Locale.ROOT);
		TextColor parsed = TextColor.fromHexString(value);
		if (parsed == null && value.matches("[0-9a-f]{6}")) {
			parsed = TextColor.fromHexString("#" + value);
		}
		if (parsed == null) {
			parsed = NamedTextColor.NAMES.value(value);
		}
		if (parsed == null) {
			throw new IllegalArgumentException("Unknown gradient color: " + color);
		}
		return parsed.asHexString();
	}

	private void requireOpen() {
		if (this.closed) {
			throw new IllegalStateException("RosaMessenger has already been closed");
		}
	}

	private final class TitleAnimation implements Runnable {

		private final Player player;
		private final UUID playerId;
		private final String title;
		private final String subtitle;
		private final List<String> titleFrames;
		private final List<String> subtitleFrames;
		private final int totalFrames;
		private final int ticksPerCharacter;
		private int frame;
		private RosaTask task;
		private boolean finished;

		private TitleAnimation(Player player, String title, String subtitle, int ticksPerCharacter) {
			this.player = player;
			this.playerId = player.getUniqueId();
			this.title = title;
			this.subtitle = subtitle;
			this.titleFrames = animationFrames(title);
			this.subtitleFrames = animationFrames(subtitle);
			this.totalFrames = Math.max(this.titleFrames.size(), this.subtitleFrames.size());
			this.ticksPerCharacter = ticksPerCharacter;
		}

		@Override
		public synchronized void run() {
			if (this.finished) return;
			if (closed || !this.player.isOnline()) {
				cancel();
				return;
			}

			this.frame++;
			if (this.frame >= this.totalFrames) {
				complete();
				return;
			}

			showTitle(
					this.player,
					frameText(this.titleFrames, this.frame),
					frameText(this.subtitleFrames, this.frame),
					0,
					this.ticksPerCharacter + 1,
					0
			);
		}

		private synchronized void bind(RosaTask task) {
			this.task = Objects.requireNonNull(task, "task");
			if (this.finished) {
				if (!task.isCancelled()) task.cancel();
				return;
			}
			if (task.isCancelled() || !task.isScheduled()) cancel();
		}

		private synchronized boolean isEmpty() {
			return this.totalFrames == 0;
		}

		private synchronized void complete() {
			if (this.finished) return;
			showTitle(
					this.player,
					this.title,
					this.subtitle,
					0,
					DEFAULT_STAY_TICKS,
					DEFAULT_FADE_OUT_TICKS
			);
			finish();
		}

		private synchronized void cancel() {
			if (this.finished) return;
			finish();
		}

		private void finish() {
			this.finished = true;
			titleAnimations.remove(this.playerId, this);
			if (this.task != null && !this.task.isCancelled()) this.task.cancel();
		}
	}

	private static String frameText(List<String> frames, int frame) {
		if (frames.isEmpty()) return "";
		return frames.get(Math.min(frame, frames.size()) - 1);
	}
}
