package net.portalmod.core.util;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistryTest {
    @Test
    public void registersEachEntryOnlyOnceEvenWhenResolvedBeforeBootstrap() {
        Map<String, Object> registered = new LinkedHashMap<>();
        AtomicInteger calls = new AtomicInteger();
        Registry<Object> registry =
                new Registry<>(
                        (id, value) -> {
                            calls.incrementAndGet();
                            registered.put(id, value);
                        });
        Registry.Entry<Object> entry = registry.register("portal", Object::new);
        assertTrue(registered.isEmpty());
        Object first = entry.get();
        registry.registerAll();
        assertSame(first, entry.get());
        assertSame(first, registered.get("portal"));
        assertEquals(1, calls.get());
    }

    @Test
    public void preservesRegistrationOrder() {
        Map<String, Object> registered = new LinkedHashMap<>();
        Registry<Object> registry = new Registry<>(registered::put);
        registry.register("z", Object::new);
        registry.register("a", Object::new);
        registry.registerAll();
        assertArrayEquals(new String[] {"z", "a"}, registered.keySet().toArray());
    }
}
