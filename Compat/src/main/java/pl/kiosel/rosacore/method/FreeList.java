package pl.kiosel.playerlist.internal;

import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unchecked")
public class FreeList implements List<Object> {

    private final List<Object> list;
    private final boolean safeMode;
    private final boolean compute;
    
    public FreeList() {
        this(new ArrayList<>());
    }
    
    public FreeList(List<?> parent) {
        this(parent, false);
    }
    
    public FreeList(List<?> parent, boolean safe) {
        this(parent, safe, false);
    }
    
    public FreeList(List<?> parent, boolean safe, boolean compute) {
        this.list = (List<Object>)parent;
        this.safeMode = safe;
        this.compute = compute;
    }
    
    public FreeList(Object parent) {
        this(parent, false, false);
    }
    
    public FreeList(Object parent, boolean safe) {
        this(parent, safe, false);
    }
    
    public FreeList(Object parent, boolean safe, boolean compute) {
        this((List<?>)parent, safe, compute);
    }
    
    @Override
    public void add(int index, Object element) {
        this.list.add(index, element);
    }
    
    @Override
    public boolean add(Object e) {
        return this.list.add(e);
    }
    
    @Override
    public boolean addAll(@NotNull Collection<?> c) {
        return this.list.addAll(c);
    }
    
    @Override
    public boolean addAll(int index, @NotNull Collection<?> c) {
        return this.list.addAll(index, c);
    }
    
    @Override
    public void clear() {
        this.list.clear();
    }
    
    @Override
    public boolean contains(Object o) {
        return this.list.contains(o);
    }
    
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return new HashSet<>(this.list).containsAll(c);
    }
    
    @Override
    public Object get(int index) {
        return this.list.get(index);
    }
    
    public byte getB(int index) {
        Object value = this.get(index);
        return (value instanceof Byte) ? ((byte)value) : (this.safeMode ? 0 : ((byte)value));
    }
    
    public Byte getByte(int index) {
        Object value = this.get(index);
        return (Byte)((value instanceof Byte) ? value : (this.safeMode ? (0) : null));
    }
    
    public char getC(int index) {
        Object value = this.get(index);
        return (char)((value instanceof Character) ? value : (this.safeMode ? '\0' : value));
    }
    
    public Character getCharacter(int index) {
        Object value = this.get(index);
        return (Character)((value instanceof Character) ? value : (this.safeMode ? Character.valueOf('\0') : null));
    }
    
    public double getD(int index) {
        Object value = this.get(index);
        return (double)((value instanceof Double) ? value : (this.safeMode ? 0.0 : value));
    }
    
    public Double getDouble(int index) {
        Object value = this.get(index);
        return (Double)((value instanceof Double) ? value : (this.safeMode ? Double.valueOf(0.0) : null));
    }
    
    public float getF(int index) {
        Object value = this.get(index);
        return (float)((value instanceof Float) ? value : (this.safeMode ? 0.0f : value));
    }
    
    public Float getFloat(int index) {
        Object value = this.get(index);
        return (Float)((value instanceof Float) ? value : (this.safeMode ? Float.valueOf(0.0f) : null));
    }
    
    public int getI(int index) {
        Object value = this.get(index);
        return (int)((value instanceof Integer) ? value : (this.safeMode ? 0 : value));
    }
    
    public Integer getInteger(int index) {
        Object value = this.get(index);
        return (Integer)((value instanceof Integer) ? value : (this.safeMode ? Integer.valueOf(0) : null));
    }
    
    public long getL(int index) {
        Object value = this.get(index);
        return (long)((value instanceof Long) ? value : (this.safeMode ? 0L : value));
    }
    
    public <X> FreeList getList(int index) {
        Object value = this.get(index);
        return (FreeList)((value instanceof FreeList) ? value : ((value instanceof List) ? new FreeList((List<?>)value, this.safeMode, this.compute) : (this.safeMode ? new FreeList() : null)));
    }
    
    public Long getLong(int index) {
        Object value = this.get(index);
        return (Long)((value instanceof Long) ? value : (this.safeMode ? Long.valueOf(0L) : null));
    }
    
    public FreeMap getMap(int index) {
        Object value = this.get(index);
        return (FreeMap)((value instanceof FreeMap) ? value : ((value instanceof Map) ? new FreeMap((Map<?, ?>)value, this.safeMode, this.compute) : (this.safeMode ? new FreeMap() : null)));
    }
    
    public short getS(int index) {
        Object value = this.get(index);
        return (value instanceof Short) ? ((short)value) : (this.safeMode ? 0 : ((short)value));
    }
    
    public Short getShort(int index) {
        Object value = this.get(index);
        return (Short)((value instanceof Short) ? value : (this.safeMode ? (0) : null));
    }
    
    public String getString(int index) {
        Object value = this.get(index);
        return (String)((value instanceof String) ? value : (this.safeMode ? ((value == null) ? "" : String.valueOf(value)) : null));
    }
    
    @Override
    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }
    
    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }
    
    @Override
    public Iterator<Object> iterator() {
        return this.list.iterator();
    }
    
    @Override
    public int lastIndexOf(Object o) {
        return this.list.lastIndexOf(o);
    }
    
    @Override
    public ListIterator<Object> listIterator() {
        return this.list.listIterator();
    }
    
    @Override
    public ListIterator<Object> listIterator(int index) {
        return this.list.listIterator(index);
    }
    
    @Override
    public Object remove(int index) {
        return this.list.remove(index);
    }
    
    @Override
    public boolean remove(Object o) {
        return this.list.remove(o);
    }
    
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.list.removeAll(c);
    }
    
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.list.retainAll(c);
    }
    
    @Override
    public Object set(int index, Object element) {
        return this.list.set(index, element);
    }
    
    @Override
    public int size() {
        return this.list.size();
    }
    
    @Override
    public FreeList subList(int fromIndex, int toIndex) {
        return new FreeList(this.list.subList(fromIndex, toIndex));
    }
    
    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }
    
    @Override
    public <X> X[] toArray(X @NotNull [] a) {
        return this.list.toArray(a);
    }
}
