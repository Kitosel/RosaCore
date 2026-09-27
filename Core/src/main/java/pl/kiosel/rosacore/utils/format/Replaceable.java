package pl.kiosel.rosacore.utils.format;

import net.kyori.adventure.text.Component;

import java.util.Locale;

public interface Replaceable {

	String replace(Locale locale, String text);

	default String replace(String text) {
		return replace(null, text);
	}

	Component replace(Locale locale, Component text);

	default Component replace(Component text) {
		return replace(null, text);
	}
}
