package pl.kiosel.rosacore.config.setting;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class SettingsYamlTemplate {

	private SettingsYamlTemplate() {
	}

	public static String render(SettingsSchema schema) {
		Objects.requireNonNull(schema, "schema");
		Node root = new Node();
		for (SettingKey<?> setting : schema.getSettings()) {
			Node current = root;
			String[] sections = setting.getPath().split("\\.");
			for (String section : sections) {
				current = current.children.computeIfAbsent(section, ignored -> new Node());
			}
			current.setting = setting;
		}

		StringBuilder yaml = new StringBuilder();
		yaml.append("# Generated from the RosaCore settings schema.\n");
		renderChildren(yaml, root, 0);
		return yaml.toString();
	}

	public static void write(Path destination, SettingsSchema schema) throws IOException {
		Objects.requireNonNull(destination, "destination");
		Path absolute = destination.toAbsolutePath().normalize();
		Path parent = absolute.getParent();
		if (parent == null) {
			throw new IllegalArgumentException("Destination must have a parent directory");
		}
		Files.createDirectories(parent);
		byte[] content = render(schema).getBytes(StandardCharsets.UTF_8);
		Path temporary = Files.createTempFile(parent, absolute.getFileName().toString() + ".", ".tmp");
		try {
			Files.write(temporary, content);
			try {
				Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void renderChildren(StringBuilder yaml, Node parent, int depth) {
		for (Map.Entry<String, Node> entry : parent.children.entrySet()) {
			Node node = entry.getValue();
			if (node.setting != null) {
				renderDescription(yaml, node.setting, depth);
				renderValue(yaml, entry.getKey(), node.setting.encodedDefaultValue(), depth);
			} else {
				indent(yaml, depth).append(renderKey(entry.getKey())).append(":\n");
				renderChildren(yaml, node, depth + 1);
			}
		}
	}

	private static void renderDescription(StringBuilder yaml, SettingKey<?> setting, int depth) {
		if (setting.isSensitive()) {
			indent(yaml, depth).append("# Sensitive value. Do not share it.\n");
		}
		for (String description : setting.getDescription()) {
			String[] lines = description.replace("\r", "").split("\n", -1);
			for (String line : lines) {
				indent(yaml, depth).append(line.isEmpty() ? "#" : "# " + line).append('\n');
			}
		}
	}

	private static void renderValue(StringBuilder yaml, String key, Object value, int depth) {
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.isEmpty()) {
				indent(yaml, depth).append(renderKey(key)).append(": []\n");
				return;
			}
			indent(yaml, depth).append(renderKey(key)).append(":\n");
			for (Object element : list) {
				indent(yaml, depth + 1).append("- ").append(renderScalar(element)).append('\n');
			}
			return;
		}
		indent(yaml, depth).append(renderKey(key)).append(": ")
				.append(renderScalar(value)).append('\n');
	}

	private static String renderScalar(Object value) {
		if (value instanceof String || value instanceof Character) {
			return quote(String.valueOf(value));
		}
		if (value instanceof Boolean || value instanceof Byte || value instanceof Short
				|| value instanceof Integer || value instanceof Long
				|| value instanceof BigInteger || value instanceof BigDecimal) {
			return String.valueOf(value).toLowerCase(Locale.ROOT);
		}
		if (value instanceof Float || value instanceof Double) {
			double number = ((Number) value).doubleValue();
			if (!Double.isNaN(number) && !Double.isInfinite(number)) {
				return String.valueOf(value);
			}
		}
		throw new IllegalArgumentException("Unsupported YAML template value: "
				+ (value == null ? "null" : value.getClass().getName()));
	}

	private static String renderKey(String key) {
		if (key.matches("[A-Za-z_][A-Za-z0-9 _-]*") && !isReserved(key)) {
			return key;
		}
		return quote(key);
	}

	private static boolean isReserved(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		return lower.equals("true") || lower.equals("false") || lower.equals("yes")
				|| lower.equals("no") || lower.equals("on") || lower.equals("off")
				|| lower.equals("null") || lower.equals("~");
	}

	private static String quote(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	private static StringBuilder indent(StringBuilder yaml, int depth) {
		for (int index = 0; index < depth; index++) {
			yaml.append("  ");
		}
		return yaml;
	}

	private static final class Node {

		private final Map<String, Node> children = new LinkedHashMap<>();
		private SettingKey<?> setting;
	}
}
