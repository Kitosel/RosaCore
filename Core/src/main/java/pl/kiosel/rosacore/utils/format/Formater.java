package pl.kiosel.rosacore.utils.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Formater {

	private final List<String[]> replacements = new ArrayList<>();

	public Formater register(String placeholder, Object value) {
		this.replacements.add(new String[]{Objects.requireNonNull(placeholder, "placeholder"), String.valueOf(value)});
		return this;
	}

	public String replace(String text) {
		if (text == null || text.isEmpty()) return "";
		for (String[] replacement : this.replacements) text = text.replace(replacement[0], replacement[1]);
		return text;
	}

	public static Formater of(String placeholder, Object value) {
		return new Formater().register(placeholder, value);
	}

	public static String format(String text, String placeholder, Object value) {
		return of(placeholder, value).replace(text);
	}
}
