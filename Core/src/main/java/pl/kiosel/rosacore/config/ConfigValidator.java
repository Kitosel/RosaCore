package pl.kiosel.rosacore.config;

@FunctionalInterface
public interface ConfigValidator {

	void validate(ConfigView config, ValidationContext context);
}
