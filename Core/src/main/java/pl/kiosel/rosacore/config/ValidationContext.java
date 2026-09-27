package pl.kiosel.rosacore.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ValidationContext {

    private final List<ConfigProblem> problems = new ArrayList<>();

    public void error(String path, String message) {
        this.problems.add(new ConfigProblem(path, Objects.requireNonNull(message, "message")));
    }

    public boolean isValid() {
        return this.problems.isEmpty();
    }

    public List<ConfigProblem> getProblems() {
        return Collections.unmodifiableList(new ArrayList<>(this.problems));
    }
}
