package dev.objz.commandbridge.scripting.bind;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class TypeAdapterRegistry {
    private final List<TypeAdapter<?>> adapters = new ArrayList<>();

    public TypeAdapterRegistry register(TypeAdapter<?> adapter) {
        adapters.add(adapter);
        return this;
    }

    public <T> TypeAdapter<T> find(Type targetType) {
        for (TypeAdapter<?> a : adapters) {
            if (a.supports(targetType))
                return (TypeAdapter<T>) a;
        }
        throw new IllegalArgumentException("No TypeAdapter for " + targetType.getTypeName());
    }
}
