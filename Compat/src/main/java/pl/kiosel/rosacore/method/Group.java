package pl.kiosel.playerlist.internal;

import lombok.Getter;

import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class Group {
    @Getter
    private final String name;
    protected List<Object> objects;
    
    public Group(String n) {
        this.objects = new ArrayList<>();
        this.name = n;
    }
    
    public <T> List<T> fetch(Predicate<T> pre) {
        List<T> objs = new ArrayList<T>();
        for (int i = this.objects.size() - 1; i >= 0; --i) {
            Object obj = this.objects.get(i);
            try {
                T o = (T)obj;
                if (pre.test(o)) {
                    objs.add(o);
                    this.objects.remove(i);
                }
            } catch (ClassCastException ignored) {}
        }
        return objs;
    }

}
