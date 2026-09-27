package pl.kiosel.rosacore.method;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class FreeMap implements Map<Object, Object> {
	private final Map<Object, Object> map;
	private final boolean safeMode;
	private final boolean compute;

	public FreeMap() {
		this(new HashMap<>());
	}

	public FreeMap(Map<?, ?> parent) {
		this(parent, false);
	}

	public FreeMap(Map<?, ?> parent, boolean safeMode) {
		this(parent, safeMode, false);
	}

	public FreeMap(Map<?, ?> parent, boolean safeMode, boolean compute) {
		this.map = (Map<Object, Object>) parent;
		this.safeMode = safeMode;
		this.compute = compute;
	}

	public FreeMap(Object parent) {
		this((Map<?, ?>) parent);
	}

	public FreeMap(Object parent, boolean safeMode) {
		this((Map<?, ?>) parent, safeMode);
	}

	public FreeMap(Object parent, boolean safeMode, boolean compute) {
		this((Map<?, ?>) parent, safeMode, compute);
	}

	@Override
	public void clear() {
		this.map.clear();
	}

	private <X> X compute(Object key, X x) {
		if (this.compute) {
			try {
				this.put(key, x);
			} catch (ClassCastException ignored) {
			}
		}
		return x;
	}

	@Override
	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.map.containsValue(value);
	}

	@Override
	public Set<Entry<Object, Object>> entrySet() {
		return this.map.entrySet();
	}

	@Override
	public Object get(Object key) {
		return this.map.get(key);
	}

	public byte getB(Object key) {
		Object value = this.get(key);
		return (byte) ((value instanceof Byte) ? value : (this.safeMode ? this.compute(key, 0) : ((byte) value)));
	}

	public Byte getByte(Object key) {
		Object value = this.get(key);
		return (Byte) ((value instanceof Byte) ? value : (this.safeMode ? this.compute(key, 0) : null));
	}

	public char getC(Object key) {
		Object value = this.get(key);
		return (char) ((value instanceof Character) ? value : (this.safeMode ? this.compute(key, '\0') : ((char) value)));
	}

	public Character getCharacter(Object key) {
		Object value = this.get(key);
		return (Character) ((value instanceof Character) ? value : (this.safeMode ? this.compute(key, '\0') : null));
	}

	public double getD(Object key) {
		Object value = this.get(key);
		return (double) ((value instanceof Double) ? value : (this.safeMode ? this.compute(key, 0) : ((double) value)));
	}

	public Double getDouble(Object key) {
		Object value = this.get(key);
		return (Double) ((value instanceof Double) ? value : (this.safeMode ? this.compute(key, 0.0) : null));
	}

	public float getF(Object key) {
		Object value = this.get(key);
		return (float) ((value instanceof Float) ? value : (this.safeMode ? this.compute(key, 0) : ((float) value)));
	}

	public Float getFloat(Object key) {
		Object value = this.get(key);
		return (Float) ((value instanceof Float) ? value : (this.safeMode ? this.compute(key, 0.0f) : null));
	}

	public int getI(Object key) {
		Object value = this.get(key);
		return (int) ((value instanceof Integer) ? value : (this.safeMode ? this.compute(key, 0) : ((int) value)));
	}

	public Integer getInteger(Object key) {
		Object value = this.get(key);
		return (Integer) ((value instanceof Integer) ? value : (this.safeMode ? this.compute(key, 0) : null));
	}

	public long getL(Object key) {
		Object value = this.get(key);
		return (long) ((value instanceof Long) ? value : (this.safeMode ? this.compute(key, 0) : ((long) value)));
	}

	public FreeList getList(Object key) {
		Object value = this.get(key);
		return (FreeList) ((value instanceof FreeList) ? value : ((value instanceof List) ? new FreeList((List<?>) value, this.safeMode, this.compute) : (this.safeMode ? new FreeList() : null)));
	}

	public Long getLong(Object key) {
		Object value = this.get(key);
		return (Long) ((value instanceof Long) ? value : (this.safeMode ? this.compute(key, 0L) : null));
	}

	public <K1, V1> FreeMap getMap(Object key) {
		Object value = this.get(key);
		return (FreeMap) ((value instanceof FreeMap) ? value : ((value instanceof Map) ? new FreeMap((Map<?, ?>) value, this.safeMode, this.compute) : (this.safeMode ? new FreeMap() : null)));
	}

	public short getS(Object key) {
		Object value = this.get(key);
		return (short) ((value instanceof Short) ? value : (this.safeMode ? this.compute(key, 0) : ((short) value)));
	}

	public Short getShort(Object key) {
		Object value = this.get(key);
		return (Short) ((value instanceof Short) ? value : (this.safeMode ? this.compute(key, 0) : null));
	}

	public String getString(Object key) {
		Object value = this.get(key);
		return (String) ((value instanceof String) ? value : (this.safeMode ? ((value == null) ? "" : this.compute(key, String.valueOf(value))) : null));
	}

	public <T> void ifPresent(Object key, Consumer<T> cons) {
		Object value = this.get(key);
		if (value != null) {
			cons.accept((T) value);
		}
	}

	public <T> void consume(Object key, Consumer<T> cons) {
		cons.accept((T) this.get(key));
	}

	public <T> Optional<T> optional(Object key) {
		return (Optional<T>) Optional.ofNullable(this.get(key));
	}

	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public boolean isSafe() {
		return this.safeMode;
	}

	@Override
	public Set<Object> keySet() {
		return this.map.keySet();
	}

	@Override
	public Object put(Object key, Object value) {
		return this.map.put(key, value);
	}

	@Override
	public void putAll(Map<?, ?> m) {
		this.map.putAll(m);
	}

	public FreeMap putThen(Object key, Object value) {
		this.put(key, value);
		return this;
	}

	@Override
	public Object remove(Object key) {
		return this.map.remove(key);
	}

	public boolean shouldCompute() {
		return this.compute;
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return this.map.toString();
	}

	public Map<?, ?> unwrap() {
		return this.map;
	}

	@Override
	public Collection<Object> values() {
		return this.map.values();
	}
}
