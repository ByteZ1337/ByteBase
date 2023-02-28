package xyz.xenondevs.bytebase.patch.util.weak;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntConsumer;

public class IntWeakIdentityHashMap<K> implements Object2IntMap<K>, WeakIdentityMap {
    
    private final Object2IntMap<WeakKey<K>> map = new Object2IntOpenHashMap<>();
    private final transient ReferenceQueue<K> queue = new ReferenceQueue<>();
    
    private final IntConsumer onRemove;
    
    public IntWeakIdentityHashMap(IntConsumer onRemove) {
        this.onRemove = onRemove;
    }
    
    private Object2IntMap<WeakKey<K>> getMap() {
        checkQueue();
        return map;
    }
    
    @SuppressWarnings({"ReassignedVariable"})
    @Override
    public void checkQueue() {
        synchronized (this) {
            for (Reference<? extends K> ref; (ref = this.queue.poll()) != null; ) {
                onRemove.accept(map.removeInt(ref));
            }
        }
    }
    
    @Override
    public int size() {
        return getMap().size();
    }
    
    @Override
    public boolean isEmpty() {
        return getMap().isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return getMap().containsKey(new WeakKey<>(key, null));
    }
    
    @Override
    public boolean containsValue(int value) {
        return getMap().containsValue(value);
    }
    
    @Override
    public int getInt(Object key) {
        return getMap().getInt(new WeakKey<>(key, null));
    }
    
    @Override
    public int put(K key, int value) {
        return getMap().put(new WeakKey<>(key, queue), value);
    }
    
    @Override
    public int removeInt(Object key) {
        return getMap().removeInt(new WeakKey<>(key, null));
    }
    
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends Integer> m) {
        for (Map.Entry<? extends K, ? extends Integer> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue().intValue());
        }
    }
    
    public void putAll(@NotNull Object2IntMap<? extends K> m) {
        for (Entry<? extends K> entry : m.object2IntEntrySet()) {
            put(entry.getKey(), entry.getIntValue());
        }
    }
    
    @Override
    public void clear() {
        getMap().clear();
    }
    
    @Override
    public void defaultReturnValue(int rv) {
        getMap().defaultReturnValue(rv);
    }
    
    @Override
    public int defaultReturnValue() {
        return getMap().defaultReturnValue();
    }
    
    @NotNull
    @Override
    public ObjectSet<K> keySet() {
        return new WrappedWeakKeySet<>(this::getMap);
    }
    
    @NotNull
    @Override
    public IntCollection values() {
        return getMap().values();
    }
    
    @NotNull
    @Override
    public ObjectSet<Entry<K>> object2IntEntrySet() {
        return new AbstractObjectSet<>() {
            
            @Override
            public ObjectIterator<Entry<K>> iterator() {
                final Iterator<Entry<WeakKey<K>>> iterator = getMap().object2IntEntrySet().iterator();
                return new AbstractObjectIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
                    
                    @Override
                    public Entry<K> next() {
                        return new Entry<>() {
                            private final Entry<WeakKey<K>> entry = iterator.next();
                            
                            @Override
                            public K getKey() {
                                return entry.getKey().get();
                            }
                            
                            @Override
                            public int getIntValue() {
                                return entry.getIntValue();
                            }
                            
                            @Override
                            public int setValue(int value) {
                                throw new UnsupportedOperationException();
                            }
                            
                        };
                    }
                };
            }
            
            @Override
            public int size() {
                return getMap().object2IntEntrySet().size();
            }
            
        };
    }
    
}
