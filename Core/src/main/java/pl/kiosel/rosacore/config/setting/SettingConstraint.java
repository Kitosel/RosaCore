package pl.kiosel.rosacore.config.setting;

@FunctionalInterface
public interface SettingConstraint<T> {

	String validate(T value);
}
