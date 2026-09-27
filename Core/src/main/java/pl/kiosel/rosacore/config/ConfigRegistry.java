package pl.kiosel.rosacore.config;

import java.nio.file.Path;
import java.util.*;

public final class ConfigRegistry {

	private final List<Registration> reloadOrder;
	private final Map<String, Registration> byId;

	private ConfigRegistry(Builder builder) {
		this.reloadOrder = Collections.unmodifiableList(resolveOrder(builder.registrations));
		Map<String, Registration> byId = new LinkedHashMap<>();
		for (Registration registration : this.reloadOrder) {
			byId.put(registration.id, registration);
		}
		this.byId = Collections.unmodifiableMap(byId);
	}

	public static Builder builder() {
		return new Builder();
	}

	public ConfigBatchResult loadAll() {
		return this.reloadAll();
	}

	public ConfigBatchResult reloadAll() {
		return this.reloadAll(ConfigReloadPolicy.CONTINUE_INDEPENDENT);
	}

	public ConfigBatchResult reloadAll(ConfigReloadPolicy policy) {
		Objects.requireNonNull(policy, "policy");
		List<ConfigBatchEntry> results = new ArrayList<>();
		Map<String, ConfigBatchEntry> completed = new LinkedHashMap<>();
		boolean halted = false;

		for (Registration registration : this.reloadOrder) {
			String skipReason = null;
			if (halted) {
				skipReason = "A previous configuration failed and the policy stops further reloads";
			} else {
				for (String dependency : registration.dependencies) {
					ConfigBatchEntry dependencyResult = completed.get(dependency);
					if (dependencyResult == null || !dependencyResult.isSuccess()) {
						skipReason = "Dependency '" + dependency + "' did not reload successfully";
						break;
					}
				}
			}

			ConfigBatchEntry entry;
			if (skipReason != null) {
				entry = ConfigBatchEntry.skipped(registration.id,
						registration.source.getPath(), skipReason);
			} else {
				ConfigLoadResult loadResult;
				try {
					loadResult = Objects.requireNonNull(registration.source.reload(),
							"ReloadableConfig returned null");
				} catch (RuntimeException exception) {
					loadResult = ConfigLoadResult.failure(0, 0,
							Collections.singletonList(new ConfigProblem(registration.id,
									"Unexpected reload failure")), exception);
				}
				entry = ConfigBatchEntry.completed(registration.id,
						registration.source.getPath(), loadResult);
				if (!entry.isSuccess() && policy == ConfigReloadPolicy.STOP_ON_FAILURE) {
					halted = true;
				}
			}
			results.add(entry);
			completed.put(registration.id, entry);
		}
		return new ConfigBatchResult(results);
	}

	public List<String> getReloadOrder() {
		return Collections.unmodifiableList(new ArrayList<>(this.byId.keySet()));
	}

	public ReloadableConfig get(String id) {
		Registration registration = this.byId.get(id);
		if (registration == null) {
			throw new IllegalArgumentException("Unknown configuration: " + id);
		}
		return registration.source;
	}

	private static List<Registration> resolveOrder(Map<String, Registration> registrations) {
		for (Registration registration : registrations.values()) {
			for (String dependency : registration.dependencies) {
				if (!registrations.containsKey(dependency)) {
					throw new IllegalStateException("Configuration '" + registration.id
							+ "' depends on missing configuration '" + dependency + "'");
				}
			}
		}

		List<Registration> ordered = new ArrayList<>();
		Set<String> resolved = new LinkedHashSet<>();
		while (ordered.size() < registrations.size()) {
			boolean progress = false;
			for (Registration registration : registrations.values()) {
				if (resolved.contains(registration.id)
						|| !resolved.containsAll(registration.dependencies)) {
					continue;
				}
				ordered.add(registration);
				resolved.add(registration.id);
				progress = true;
			}
			if (!progress) {
				Set<String> unresolved = new LinkedHashSet<>(registrations.keySet());
				unresolved.removeAll(resolved);
				throw new IllegalStateException("Configuration dependency cycle: " + unresolved);
			}
		}
		return ordered;
	}

	public static final class Builder {

		private final Map<String, Registration> registrations = new LinkedHashMap<>();
		private final Map<Path, String> paths = new LinkedHashMap<>();

		public Builder register(String id, ReloadableConfig source, String... dependencies) {
			if (id == null || !id.matches("[A-Za-z0-9._-]+")) {
				throw new IllegalArgumentException("Invalid configuration id: " + id);
			}
			Objects.requireNonNull(source, "source");
			Objects.requireNonNull(dependencies, "dependencies");
			if (this.registrations.containsKey(id)) {
				throw new IllegalArgumentException("Duplicate configuration id: " + id);
			}

			Path path = Objects.requireNonNull(source.getPath(), "source path")
					.toAbsolutePath().normalize();
			String pathOwner = this.paths.get(path);
			if (pathOwner != null) {
				throw new IllegalArgumentException("Configuration path is already registered by '"
						+ pathOwner + "': " + path);
			}

			Set<String> dependencySet = new LinkedHashSet<>();
			for (String dependency : dependencies) {
				if (dependency == null || dependency.isEmpty()) {
					throw new IllegalArgumentException("Dependency id cannot be blank");
				}
				if (id.equals(dependency)) {
					throw new IllegalArgumentException("Configuration cannot depend on itself: " + id);
				}
				dependencySet.add(dependency);
			}
			this.registrations.put(id, new Registration(id, source, dependencySet));
			this.paths.put(path, id);
			return this;
		}

		public ConfigRegistry build() {
			return new ConfigRegistry(this);
		}
	}

	private static final class Registration {

		private final String id;
		private final ReloadableConfig source;
		private final Set<String> dependencies;

		private Registration(String id, ReloadableConfig source, Set<String> dependencies) {
			this.id = id;
			this.source = source;
			this.dependencies = Collections.unmodifiableSet(new LinkedHashSet<>(dependencies));
		}
	}
}
