package pl.kiosel.rosacore.message;

import pl.kiosel.rosacore.config.ConfigLoadResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessageLoadResult {

    private final boolean success;
    private final String requestedLocale;
    private final String activeLocale;
    private final String problem;
    private final Throwable cause;
    private final ConfigLoadResult fallbackResult;
    private final ConfigLoadResult localeResult;
    private final List<String> missingRequiredKeys;

    private MessageLoadResult(boolean success, String requestedLocale, String activeLocale,
                              String problem, Throwable cause, ConfigLoadResult fallbackResult,
                              ConfigLoadResult localeResult, List<String> missingRequiredKeys) {
        this.success = success;
        this.requestedLocale = requestedLocale;
        this.activeLocale = activeLocale;
        this.problem = problem;
        this.cause = cause;
        this.fallbackResult = fallbackResult;
        this.localeResult = localeResult;
        this.missingRequiredKeys = Collections.unmodifiableList(new ArrayList<>(missingRequiredKeys));
    }

    static MessageLoadResult success(String requestedLocale, ConfigLoadResult fallbackResult,
                                     ConfigLoadResult localeResult, List<String> missingRequiredKeys) {
        return new MessageLoadResult(true, requestedLocale, requestedLocale,
                null, null, fallbackResult, localeResult, missingRequiredKeys);
    }

    static MessageLoadResult failure(String requestedLocale, String activeLocale, String problem,
                                     Throwable cause, ConfigLoadResult fallbackResult,
                                     ConfigLoadResult localeResult) {
        return new MessageLoadResult(false, requestedLocale, activeLocale, problem, cause,
                fallbackResult, localeResult, Collections.emptyList());
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getRequestedLocale() {
        return this.requestedLocale;
    }

    public String getActiveLocale() {
        return this.activeLocale;
    }

    public String getProblem() {
        return this.problem;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public ConfigLoadResult getFallbackResult() {
        return this.fallbackResult;
    }

    public ConfigLoadResult getLocaleResult() {
        return this.localeResult;
    }

    public List<String> getMissingRequiredKeys() {
        return this.missingRequiredKeys;
    }
}
