package pl.kiosel.rosacore.message;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.text.Normalizer;
import java.util.*;
import java.util.logging.Logger;

public final class AsciiArtCreator {

	private static final int FONT_HEIGHT = 2;
	private static final Map<Character, String[]> FONT = createFont();

	private final List<String> lines = new ArrayList<>();
	private String glyphSeparator = " ";

	private AsciiArtCreator() {
	}

	public static AsciiArtCreator create() {
		return new AsciiArtCreator();
	}

	public static AsciiArtCreator of(String text) {
		return create().ascii(text);
	}

	public AsciiArtCreator ascii(String text) {
		Objects.requireNonNull(text, "text");
		String[] textLines = text.replace("\r", "").split("\n", -1);
		for (String textLine : textLines) {
			if (textLine.isEmpty()) {
				this.blankLine();
				continue;
			}
			this.lines.addAll(render(textLine, this.glyphSeparator));
		}
		return this;
	}

	public AsciiArtCreator text(String text) {
		return this.ascii(text);
	}

	public AsciiArtCreator line(String line) {
		this.lines.add(Objects.requireNonNull(line, "line"));
		return this;
	}

	public AsciiArtCreator lines(String... lines) {
		Objects.requireNonNull(lines, "lines");
		for (String line : lines) {
			this.line(line);
		}
		return this;
	}

	public AsciiArtCreator blankLine() {
		this.lines.add("");
		return this;
	}

	public AsciiArtCreator spacing(int spaces) {
		if (spaces < 0) {
			throw new IllegalArgumentException("ASCII art spacing cannot be negative: " + spaces);
		}
		StringBuilder separator = new StringBuilder(spaces);
		for (int index = 0; index < spaces; index++) {
			separator.append(' ');
		}
		this.glyphSeparator = separator.toString();
		return this;
	}

	public List<String> buildLines() {
		return Collections.unmodifiableList(new ArrayList<>(this.lines));
	}

	public String build() {
		return String.join("\n", this.lines);
	}

	public void log(Logger logger) {
		Objects.requireNonNull(logger, "logger").info(" \n" + this.build());
	}

	public void log(Plugin plugin) {
		this.log(Objects.requireNonNull(plugin, "plugin").getLogger());
	}

	public void send(CommandSender sender) {
		Objects.requireNonNull(sender, "sender");
		for (String line : this.lines) {
			sender.sendMessage(line);
		}
	}

	@Override
	public String toString() {
		return this.build();
	}

	private static List<String> render(String text, String separator) {
		String normalized = normalize(text);
		List<StringBuilder> rows = new ArrayList<>(FONT_HEIGHT);
		for (int row = 0; row < FONT_HEIGHT; row++) {
			rows.add(new StringBuilder());
		}

		for (int characterIndex = 0; characterIndex < normalized.length(); characterIndex++) {
			String[] glyph = FONT.get(normalized.charAt(characterIndex));
			if (glyph == null) glyph = FONT.get('?');
			for (int row = 0; row < FONT_HEIGHT; row++) {
				if (characterIndex > 0) rows.get(row).append(separator);
				rows.get(row).append(glyph[row]);
			}
		}

		List<String> rendered = new ArrayList<>(FONT_HEIGHT);
		for (StringBuilder row : rows) {
			rendered.add(row.toString());
		}
		return rendered;
	}

	private static String normalize(String text) {
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toUpperCase(Locale.ROOT);
		return normalized
				.replace('Ł', 'L')
				.replace('Đ', 'D')
				.replace('Ø', 'O')
				.replace('Ą', 'A')
				.replace('Ę', 'E')
				.replace('Ż', 'Z')
				.replace('Ź', 'Z')
				.replace('Ś', 'S')
				.replace('Ć', 'C')
				.replace('Ó', 'O')
				.replace('Ń', 'N');
	}

	private static Map<Character, String[]> createFont() {
		Map<Character, String[]> font = new LinkedHashMap<>();
		glyph(font, 'A', "▄▀█", "█▀█");
		glyph(font, 'B', "█▄▄", "█▄█");
		glyph(font, 'C', "█▀▀", "█▄▄");
		glyph(font, 'D', "█▀▄", "█▄▀");
		glyph(font, 'E', "█▀▀", "██▄");
		glyph(font, 'F', "█▀▀", "█▀░");
		glyph(font, 'G', "█▀▀", "█▄█");
		glyph(font, 'H', "█▄█", "█░█");
		glyph(font, 'I', "█", "█");
		glyph(font, 'J', "░░█", "█▄█");
		glyph(font, 'K', "█▄▀", "█░█");
		glyph(font, 'L', "█░░", "█▄▄");
		glyph(font, 'M', "█▀▄▀█", "█░▀░█");
		glyph(font, 'N', "█▄░█", "█░▀█");
		glyph(font, 'O', "█▀█", "█▄█");
		glyph(font, 'P', "█▀█", "█▀▀");
		glyph(font, 'Q', "█▀█", "█▄▀");
		glyph(font, 'R', "█▀█", "█▀▄");
		glyph(font, 'S', "█▀▀", "▄▄█");
		glyph(font, 'T', "▀█▀", "░█░");
		glyph(font, 'U', "█░█", "█▄█");
		glyph(font, 'V', "█░█", "░▀░");
		glyph(font, 'W', "█░░░█", "▀▄▀▄▀");
		glyph(font, 'X', "▀▄▀", "█░█");
		glyph(font, 'Y', "█▄█", "░█░");
		glyph(font, 'Z', "▀▀█", "█▄▄");

		glyph(font, '0', "█▀█", "█▄█");
		glyph(font, '1', "▄█", "░█");
		glyph(font, '2', "▀▀█", "█▄▄");
		glyph(font, '3', "▀▀█", "▄▄█");
		glyph(font, '4', "█░█", "▀▀█");
		glyph(font, '5', "█▀▀", "▄▄█");
		glyph(font, '6', "█▀▀", "█▄█");
		glyph(font, '7', "▀▀█", "░█░");
		glyph(font, '8', "█▀█", "█▄█");
		glyph(font, '9', "█▀█", "▄▄█");

		glyph(font, ' ', "░░░", "░░░");
		glyph(font, '.', "░", "▄");
		glyph(font, ',', "░", "▄▀");
		glyph(font, ':', "▄", "▄");
		glyph(font, ';', "▄", "▄▀");
		glyph(font, '!', "█", "▄");
		glyph(font, '?', "▀█", "░▄");
		glyph(font, '-', "▄▄▄", "░░░");
		glyph(font, '_', "░░░", "▄▄▄");
		glyph(font, '/', "░▄", "▄░");
		glyph(font, '\\', "▄░", "░▄");
		glyph(font, '|', "█", "█");
		glyph(font, '+', "▄█▄", "░█░");
		glyph(font, '=', "▄▄▄", "▀▀▀");
		glyph(font, '#', "█▄█", "█▀█");
		return Collections.unmodifiableMap(font);
	}

	private static void glyph(Map<Character, String[]> font, char character,
							  String top, String bottom) {
		font.put(character, new String[]{top, bottom});
	}
}
