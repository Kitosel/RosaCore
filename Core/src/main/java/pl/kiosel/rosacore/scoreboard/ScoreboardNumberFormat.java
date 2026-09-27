package pl.kiosel.rosacore.scoreboard;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class ScoreboardNumberFormat {

	public static final ScoreboardNumberFormat DEFAULT = new ScoreboardNumberFormat(Type.DEFAULT, null);
	public static final ScoreboardNumberFormat BLANK = new ScoreboardNumberFormat(Type.BLANK, null);

	private final Type type;
	private final String value;

	private ScoreboardNumberFormat(Type type, String value) {
		this.type = Objects.requireNonNull(type, "type");
		this.value = value;
	}

	public static ScoreboardNumberFormat fixed(String text) {
		return new ScoreboardNumberFormat(Type.FIXED, Objects.requireNonNull(text, "text"));
	}

	public static ScoreboardNumberFormat styled(String style) {
		return new ScoreboardNumberFormat(Type.STYLED, Objects.requireNonNull(style, "style"));
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof ScoreboardNumberFormat)) return false;
		ScoreboardNumberFormat other = (ScoreboardNumberFormat) object;
		return this.type == other.type && Objects.equals(this.value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.value);
	}

	@Override
	public String toString() {
		return this.value == null ? this.type.name() : this.type.name() + '(' + this.value + ')';
	}

	public enum Type {
		DEFAULT,
		BLANK,
		FIXED,
		STYLED
	}
}
