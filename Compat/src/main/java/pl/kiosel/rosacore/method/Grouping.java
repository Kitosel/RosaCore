package pl.kiosel.rosacore.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Grouping {

	private final Map<Class<?>, List<Group>> groups;

	public Grouping() {
		this.groups = new HashMap<>();
	}

	public <T> Group getGroup(Class<T> type, String name, Supplier<List<T>> defaults) {
		List<Group> group = this.groups.get(type);
		if (group == null) {
			if (defaults == null) {
				return null;
			}
			this.groups.put(type, group = new ArrayList<>());
		}
		for (int i = group.size() - 1; i >= 0; --i) {
			Group gr = group.get(i);
			if (gr.getName().equals(name)) {
				return gr;
			}
		}
		Group gr2 = new Group(name);
		gr2.objects.addAll(defaults.get());
		group.add(gr2);
		return gr2;
	}
}
