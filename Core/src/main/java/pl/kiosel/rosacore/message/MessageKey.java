package pl.kiosel.rosacore.message;

import java.util.Objects;

@FunctionalInterface
public interface MessageKey {

    String getPath();

    default String getDefaultMessage() {
        return null;
    }

    static MessageKey of(String path) {
        String checkedPath = requirePath(path);
        return () -> checkedPath;
    }

    static MessageKey of(String path, String defaultMessage) {
        String checkedPath = requirePath(path);
        Objects.requireNonNull(defaultMessage, "defaultMessage");
        return new MessageKey() {
            @Override
            public String getPath() {
                return checkedPath;
            }

            @Override
            public String getDefaultMessage() {
                return defaultMessage;
            }
        };
    }

    static String requirePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Message path cannot be blank");
        }
        return path;
    }
}
