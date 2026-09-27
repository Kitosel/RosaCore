package pl.kiosel.rosacore.message;

import lombok.Getter;

@Getter
public final class MissingMessageException extends IllegalArgumentException {

	private final String path;

	public MissingMessageException(String path) {
		super("Missing message: " + path);
		this.path = path;
	}

}
