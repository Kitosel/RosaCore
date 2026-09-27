package pl.kiosel.rosacore.config.setting;

public interface SettingCodec<T> {

	T decode(Object value) throws IllegalArgumentException;

	Object encode(T value);

	default T copy(T value) {
		return value;
	}

	String describeExpectedValue();
}
