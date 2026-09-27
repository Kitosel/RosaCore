package pl.kiosel.rosacore.message;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class MiniMessageParser {

	private static final String CLICK = "click";
	private static final String HOVER = "hover";
	private static final String FONT = "font";
	private static final String TAG_START = "<";
	private static final String TAG_END = ">";
	private static final String CLOSE_TAG = "/";
	private static final String SEPARATOR = "=";
	private static final String START = "start";
	private static final String TOKEN = "token";
	private static final String INNER = "inner";
	private static final String END = "end";
	private static final Pattern pattern;
	private static final Method CURRENT_COMPONENT_METHOD = findMethod(ComponentBuilder.class, "getCurrentComponent");
	private static final Method SET_FONT_METHOD = findMethod(BaseComponent.class, "setFont", String.class);

	static {
		pattern = Pattern.compile("((?<start><)(?<token>([^<>]+)|([^<>]+\"(?<inner>[^\"]+)\"))(?<end>>))+?");
	}

	@NotNull
	public static String escapeTokens(@NotNull String richMessage) {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = MiniMessageParser.pattern.matcher(richMessage);
		int lastEnd = 0;
		while (matcher.find()) {
			int startIndex = matcher.start();
			int endIndex = matcher.end();
			if (startIndex > lastEnd) {
				sb.append(richMessage, lastEnd, startIndex);
			}
			lastEnd = endIndex;
			String start = matcher.group(START);
			String token = matcher.group(TOKEN);
			String inner = matcher.group(INNER);
			String end = matcher.group(END);
			if (inner != null) {
				token = token.replace(inner, escapeTokens(inner));
			}
			sb.append("\\").append(start).append(token).append("\\").append(end);
		}
		if (richMessage.length() > lastEnd) {
			sb.append(richMessage.substring(lastEnd));
		}
		return sb.toString();
	}

	@NotNull
	public static String stripTokens(@NotNull String richMessage) {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = MiniMessageParser.pattern.matcher(richMessage);
		int lastEnd = 0;
		while (matcher.find()) {
			int startIndex = matcher.start();
			int endIndex = matcher.end();
			if (startIndex > lastEnd) {
				sb.append(richMessage, lastEnd, startIndex);
			}
			lastEnd = endIndex;
		}
		if (richMessage.length() > lastEnd) {
			sb.append(richMessage.substring(lastEnd));
		}
		return sb.toString();
	}

	@NotNull
	public static String handlePlaceholders(@NotNull String richMessage, @NotNull String... placeholders) {
		if (placeholders.length % 2 != 0) {
			throw new ParseException("Invalid number placeholders defined, usage: parseFormat(format, key, value, key, value...)");
		}
		for (int i = 0; i < placeholders.length; i += 2) {
			richMessage = richMessage.replace(TAG_START + placeholders[i] + TAG_END, placeholders[i + 1]);
		}
		return richMessage;
	}

	@NotNull
	public static String handlePlaceholders(@NotNull String richMessage, @NotNull Map<String, String> placeholders) {
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			richMessage = richMessage.replace(TAG_START + entry.getKey() + TAG_END, entry.getValue());
		}
		return richMessage;
	}

	@NotNull
	public static BaseComponent[] parseFormat(@NotNull String richMessage, @NotNull String... placeholders) {
		return parseFormat(handlePlaceholders(richMessage, placeholders));
	}

	@NotNull
	public static BaseComponent[] parseFormat(@NotNull String richMessage, @NotNull Map<String, String> placeholders) {
		return parseFormat(handlePlaceholders(richMessage, placeholders));
	}

	@NotNull
	public static BaseComponent[] parseFormat(@NotNull String richMessage) {
		ComponentBuilder builder = null;
		Deque<ClickEvent> clickEvents = new ArrayDeque<>();
		Deque<HoverEvent> hoverEvents = new ArrayDeque<>();
		Deque<ChatColor> colors = new ArrayDeque<>();
		Deque<String> fonts = new ArrayDeque<>();
		EnumSet<TextDecoration> decorations = EnumSet.noneOf(TextDecoration.class);
		Matcher matcher = MiniMessageParser.pattern.matcher(richMessage);
		int lastEnd = 0;
		while (matcher.find()) {
			int startIndex = matcher.start();
			int endIndex = matcher.end();
			String msg = null;
			if (startIndex > lastEnd) {
				msg = richMessage.substring(lastEnd, startIndex);
			}
			lastEnd = endIndex;
			if (msg != null && !msg.isEmpty()) {
				if (builder == null) {
					builder = new ComponentBuilder(msg);
				} else {
					builder.append(msg, ComponentBuilder.FormatRetention.NONE);
				}
				if (!clickEvents.isEmpty()) {
					builder.event(clickEvents.peek());
				}
				if (!hoverEvents.isEmpty()) {
					builder.event(hoverEvents.peek());
				}
				if (!colors.isEmpty()) {
					builder.color(colors.peek());
				}
				if (!fonts.isEmpty()) {
					applyFont(builder, fonts.peek());
				}
				if (!decorations.isEmpty()) {
					for (TextDecoration decor : decorations) {
						decor.apply(builder);
					}
				}
			}
			String token = matcher.group(TOKEN);
			String inner = matcher.group(INNER);
			if (token.startsWith(CLICK + SEPARATOR)) {
				clickEvents.push(handleClick(token, inner));
			} else if (token.equals(CLOSE_TAG + CLICK)) {
				clickEvents.pop();
			} else if (token.startsWith(HOVER + SEPARATOR)) {
				hoverEvents.push(handleHover(token, inner));
			} else if (token.equals(CLOSE_TAG + HOVER)) {
				hoverEvents.pop();
			} else if (token.startsWith(FONT + SEPARATOR)) {
				fonts.push(handleFont(token, inner));
			} else if (token.equals(CLOSE_TAG + FONT)) {
				fonts.pop();
			} else {
				Optional<TextDecoration> deco;
				if ((deco = resolveDecoration(token)).isPresent()) {
					decorations.add(deco.get());
				} else if (token.startsWith(CLOSE_TAG) && (deco = resolveDecoration(token.replace(CLOSE_TAG, ""))).isPresent()) {
					decorations.remove(deco.get());
				} else {
					Optional<ChatColor> color;
					if ((color = resolveColor(token)).isPresent()) {
						colors.push(color.get());
					} else if (token.startsWith(CLOSE_TAG) && resolveColor(token.replace(CLOSE_TAG, "")).isPresent()) {
						colors.pop();
					} else if (builder == null) {
						builder = new ComponentBuilder("<" + token + ">");
					} else {
						builder.append("<" + token + ">", ComponentBuilder.FormatRetention.NONE);
					}
				}
			}
		}
		if (richMessage.length() > lastEnd) {
			String msg2 = richMessage.substring(lastEnd);
			if (builder == null) {
				builder = new ComponentBuilder(msg2);
			} else {
				builder.append(msg2, ComponentBuilder.FormatRetention.NONE);
			}
			if (!clickEvents.isEmpty()) {
				builder.event(clickEvents.peek());
			}
			if (!hoverEvents.isEmpty()) {
				builder.event(hoverEvents.peek());
			}
			if (!colors.isEmpty()) {
				builder.color(colors.peek());
			}
			if (!fonts.isEmpty()) {
				applyFont(builder, fonts.peek());
			}
			if (!decorations.isEmpty()) {
				for (TextDecoration decor2 : decorations) {
					decor2.apply(builder);
				}
			}
		}
		if (builder == null) {
			builder = new ComponentBuilder("");
		}
		return builder.create();
	}

	private static void applyFont(ComponentBuilder builder, String font) {
		if (CURRENT_COMPONENT_METHOD == null || SET_FONT_METHOD == null) return;
		try {
			Object component = CURRENT_COMPONENT_METHOD.invoke(builder);
			if (component != null) SET_FONT_METHOD.invoke(component, font);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// Fonts do not exist in the legacy Bungee chat API used by Minecraft 1.8.
		}
	}

	private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
		try {
			return type.getMethod(name, parameters);
		} catch (ReflectiveOperationException | SecurityException ignored) {
			return null;
		}
	}

	@NotNull
	private static String handleFont(@NotNull String token, @NotNull String inner) {
		String[] args = token.split(SEPARATOR, 2);
		if (args.length < 2) {
			throw new ParseException("Can't parse font (too few args) " + token);
		}
		return args[1];
	}

	@NotNull
	private static ClickEvent handleClick(@NotNull String token, @NotNull String inner) {
		String[] args = token.split(SEPARATOR, 2);
		if (args.length < 2) {
			throw new ParseException("Can't parse click action (too few args) " + token);
		}
		ClickEvent.Action action = ClickEvent.Action.valueOf(args[1].toUpperCase(Locale.ROOT));
		return new ClickEvent(action, token.replace(CLICK + SEPARATOR + args[1] + SEPARATOR, ""));
	}

	@NotNull
	private static HoverEvent handleHover(@NotNull String token, @NotNull String inner) {
		String[] args = token.split(SEPARATOR, 2);
		if (args.length < 2) {
			throw new ParseException("Can't parse hover action (too few args) " + token);
		}
		HoverEvent.Action action = HoverEvent.Action.valueOf(args[1].toUpperCase(Locale.ROOT));
		return new HoverEvent(action, parseFormat(inner));
	}

	@NotNull
	private static Optional<ChatColor> resolveColor(@NotNull String token) {
		try {
			return Optional.of(ChatColor.valueOf(token.toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	@NotNull
	private static Optional<TextDecoration> resolveDecoration(@NotNull String token) {
		try {
			return Optional.of(TextDecoration.valueOf(token.toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	enum TextDecoration {
		BOLD(b -> b.bold(true)),
		ITALIC(b -> b.italic(true)),
		UNDERLINED(b -> b.underlined(true)),
		STRIKETHROUGH(b -> b.strikethrough(true)),
		OBFUSCATED(b -> b.obfuscated(true));

		private final Consumer<ComponentBuilder> builder;

		TextDecoration(Consumer<ComponentBuilder> builder) {
			this.builder = builder;
		}

		public void apply(@NotNull ComponentBuilder comp) {
			this.builder.accept(comp);
		}
	}

	static class ParseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ParseException(@NotNull String message) {
			super(message);
		}
	}
}
