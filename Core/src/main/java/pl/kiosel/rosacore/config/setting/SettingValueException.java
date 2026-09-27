package pl.kiosel.rosacore.config.setting;

public final class SettingValueException extends Exception {

    private final String path;

    SettingValueException(String path, String message) {
        super(message);
        this.path = path;
    }

    SettingValueException(String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
