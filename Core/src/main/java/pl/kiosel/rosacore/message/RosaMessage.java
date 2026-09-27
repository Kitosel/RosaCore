package pl.kiosel.rosacore.message;

import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RosaMessage {

	private static final Pattern PLACEHOLDER = Pattern.compile("%([A-Za-z0-9_.-]+)%");

	@Getter
	private final String path;
	private final List<String> templates;
	private final String prefix;
	private final String prefixSeparator;
	@Getter
	private final String resolvedLocale;
	@Getter
	private final boolean fallback;
	private final UnresolvedPlaceholderPolicy unresolvedPlaceholderPolicy;
	private final LegacyColorizer colorizer;
	private final MessagePlaceholders placeholders;

	RosaMessage(String path, List<String> templates, String prefix, String prefixSeparator,
				String resolvedLocale, boolean fallback, UnresolvedPlaceholderPolicy unresolvedPlaceholderPolicy,
				LegacyColorizer colorizer, MessagePlaceholders placeholders) {
		this.path = path;
		this.templates = Collections.unmodifiableList(new ArrayList<>(templates));
		this.prefix = prefix;
		this.prefixSeparator = prefixSeparator;
		this.resolvedLocale = resolvedLocale;
		this.fallback = fallback;
		this.unresolvedPlaceholderPolicy = unresolvedPlaceholderPolicy;
		this.colorizer = colorizer;
		this.placeholders = placeholders;
	}

	public RosaMessage with(String name, Object value) {
		return new RosaMessage(this.path, this.templates, this.prefix, this.prefixSeparator,
				this.resolvedLocale, this.fallback, this.unresolvedPlaceholderPolicy,
				this.colorizer, this.placeholders.with(name, value));
	}

	public RosaMessage with(MessagePlaceholders placeholders) {
		Objects.requireNonNull(placeholders, "placeholders");
		MessagePlaceholders combined = this.placeholders;
		for (java.util.Map.Entry<String, String> entry : placeholders.asMap().entrySet()) {
			combined = combined.with(entry.getKey(), entry.getValue());
		}
		return new RosaMessage(this.path, this.templates, this.prefix, this.prefixSeparator,
				this.resolvedLocale, this.fallback, this.unresolvedPlaceholderPolicy,
				this.colorizer, combined);
	}

	public RosaMessage withPairs(Object... nameValuePairs) {
		return this.with(MessagePlaceholders.pairs(nameValuePairs));
	}

	public String plain() {
		return String.join("\n", this.plainLines());
	}

	public String legacy() {
		return String.join("\n", this.legacyLines());
	}

	public String uncolored() {
		return this.colorizer.stripColors(this.plain());
	}

	public String prefixedLegacy() {
		return String.join("\n", this.prefixedLegacyLines());
	}

	public List<String> plainLines() {
		List<String> rendered = new ArrayList<>();
		for (String template : this.templates) {
			String value = this.render(template);
			Collections.addAll(rendered, value.split("\n", -1));
		}
		return Collections.unmodifiableList(rendered);
	}

	public List<String> legacyLines() {
		List<String> lines = new ArrayList<>();
		for (String line : this.plainLines()) {
			lines.add(this.colorizer.colorize(line));
		}
		return Collections.unmodifiableList(lines);
	}

	public List<String> prefixedLegacyLines() {
		List<String> lines = new ArrayList<>(this.legacyLines());
		if (!lines.isEmpty() && this.prefix != null && !this.prefix.isEmpty()) {
			String renderedPrefix = this.colorizer.colorize(this.render(this.prefix));
			lines.set(0, renderedPrefix + this.prefixSeparator + lines.get(0));
		}
		return Collections.unmodifiableList(lines);
	}

	public void send(CommandSender sender) {
		Objects.requireNonNull(sender, "sender");
		for (String line : this.legacyLines()) {
			sender.sendMessage(line);
		}
	}

	public void sendPrefixed(CommandSender sender) {
		Objects.requireNonNull(sender, "sender");
		for (String line : this.prefixedLegacyLines()) {
			sender.sendMessage(line);
		}
	}

	public List<String> getRawTemplates() {
		return this.templates;
	}

	private String render(String template) {
		String normalized = template.replace("\\n", "\n");
		Matcher matcher = PLACEHOLDER.matcher(normalized);
		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			String name = matcher.group(1);
			String replacement;
			if (this.placeholders.contains(name)) {
				replacement = this.placeholders.get(name);
			} else if (this.unresolvedPlaceholderPolicy == UnresolvedPlaceholderPolicy.EMPTY) {
				replacement = "";
			} else if (this.unresolvedPlaceholderPolicy == UnresolvedPlaceholderPolicy.THROW) {
				throw new UnresolvedPlaceholderException(name);
			} else {
				replacement = matcher.group(0);
			}
			matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(output);
		return output.toString();
	}
}
