package pl.kiosel.rosacore.scoreboard;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.lang.reflect.Method;

final class PaperScoreboardFeatures {

	private static final String NUMBER_FORMAT_CLASS =
			"io.papermc.paper.scoreboard.numbers.NumberFormat";
	private static final String COMPONENT_CLASS = adventureClass("adventure.text.Component");
	private static final String LEGACY_SERIALIZER_CLASS =
			adventureClass("adventure.text.serializer.legacy.LegacyComponentSerializer");

	private final Class<?> numberFormatClass;
	private final Class<?> componentClass;
	private final Method objectiveNumberFormat;
	private final Method scoreCustomName;
	private final Method legacySection;
	private final Method deserialize;

	PaperScoreboardFeatures(ClassLoader classLoader) {
		Class<?> foundNumberFormat = null;
		Class<?> foundComponent = null;
		Method foundObjectiveNumberFormat = null;
		Method foundScoreCustomName = null;
		Method foundLegacySection = null;
		Method foundDeserialize = null;
		try {
			foundNumberFormat = Class.forName(NUMBER_FORMAT_CLASS, false, classLoader);
			foundObjectiveNumberFormat = Objective.class.getMethod("numberFormat", foundNumberFormat);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			foundNumberFormat = null;
			foundObjectiveNumberFormat = null;
		}
		try {
			foundComponent = Class.forName(COMPONENT_CLASS, false, classLoader);
			Class<?> serializerClass = Class.forName(LEGACY_SERIALIZER_CLASS, false, classLoader);
			foundScoreCustomName = Score.class.getMethod("customName", foundComponent);
			foundLegacySection = serializerClass.getMethod("legacySection");
			foundDeserialize = serializerClass.getMethod("deserialize", String.class);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			foundComponent = null;
			foundScoreCustomName = null;
			foundLegacySection = null;
			foundDeserialize = null;
		}
		this.numberFormatClass = foundNumberFormat;
		this.componentClass = foundComponent;
		this.objectiveNumberFormat = foundObjectiveNumberFormat;
		this.scoreCustomName = foundScoreCustomName;
		this.legacySection = foundLegacySection;
		this.deserialize = foundDeserialize;
	}

	boolean isSupported() {
		return this.numberFormatClass != null && this.objectiveNumberFormat != null;
	}

	boolean setCustomName(Score score, String legacyText) {
		if (this.componentClass == null || this.scoreCustomName == null
				|| this.legacySection == null || this.deserialize == null) return false;
		try {
			this.scoreCustomName.invoke(score, component(legacyText));
			return true;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	boolean apply(Objective objective, ScoreboardNumberFormat format, String renderedValue) {
		if (!isSupported()) return false;
		try {
			Object paperFormat;
			switch (format.getType()) {
				case DEFAULT:
					paperFormat = null;
					break;
				case BLANK:
					paperFormat = invokeFactory("blank");
					break;
				case FIXED:
					paperFormat = invokeFactory("fixed", component(renderedValue));
					break;
				case STYLED:
					Object styledComponent = component(renderedValue + "0");
					Object style = this.componentClass.getMethod("style").invoke(styledComponent);
					paperFormat = invokeFactory("styled", style);
					break;
				default:
					throw new IllegalStateException("Unknown number format: " + format.getType());
			}
			this.objectiveNumberFormat.invoke(objective, paperFormat);
			return true;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	private Object component(String legacyText) throws ReflectiveOperationException {
		if (this.componentClass == null || this.legacySection == null || this.deserialize == null) {
			throw new ReflectiveOperationException("Paper Adventure components are unavailable");
		}
		Object serializer = this.legacySection.invoke(null);
		Object component = this.deserialize.invoke(serializer, legacyText == null ? "" : legacyText);
		if (!this.componentClass.isInstance(component)) {
			throw new ReflectiveOperationException("Paper serializer returned an incompatible component");
		}
		return component;
	}

	private Object invokeFactory(String name, Object... arguments) throws ReflectiveOperationException {
		for (Method method : this.numberFormatClass.getMethods()) {
			if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) continue;
			if (arguments.length == 0 || method.getParameterTypes()[0].isInstance(arguments[0])) {
				return method.invoke(null, arguments);
			}
		}
		throw new NoSuchMethodException(NUMBER_FORMAT_CLASS + '.' + name);
	}

	private static String adventureClass(String suffix) {
		return new StringBuilder(11 + suffix.length())
				.append("net").append('.').append("kyori").append('.')
				.append(suffix).toString();
	}
}
