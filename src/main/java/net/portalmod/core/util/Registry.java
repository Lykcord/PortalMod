package net.portalmod.core.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Registry<T> {
    private final BiConsumer<String, T> registrar;

    public Registry() {
        this((name, value) -> {});
    }

    public Registry(BiConsumer<String, T> registrar) {
        this.registrar = registrar;
    }

    private final Map<String, Entry<T>> REGISTRY = new LinkedHashMap<>();

    public <S extends T> Entry<S> register(String path, Supplier<S> resource) {
        Entry<S> entry =
                new Entry<>(
                        () -> {
                            S value = resource.get();
                            registrar.accept(path, value);
                            return value;
                        });
        REGISTRY.put(path, (Entry<T>) entry);
        return entry;
    }

    public void registerAll() {
        REGISTRY.forEach((name, supplier) -> supplier.get());
    }

    public Map<String, Entry<T>> getRegistry() {
        return REGISTRY;
    }

    public static class Entry<T> implements Supplier<T> {
        private final Supplier<T> supplier;
        private T value;

        public Entry(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
