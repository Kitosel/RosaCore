package pl.kiosel.rosacore.message;

import lombok.Getter;

@Getter
public final class UnresolvedPlaceholderException extends IllegalArgumentException {

	private final String placeholder;

	public UnresolvedPlaceholderException(String placeholder) {
		super("Unresolved message placeholder: " + placeholder);
		this.placeholder = placeholder;
	}

}
