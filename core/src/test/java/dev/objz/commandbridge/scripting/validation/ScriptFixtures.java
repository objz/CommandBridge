package dev.objz.commandbridge.scripting.validation;

import java.lang.reflect.RecordComponent;
import java.util.Map;

import dev.objz.commandbridge.scripting.bind.RecordBinder;

public class ScriptFixtures {

    public static RecordBinder.MutableRecordBuffer createBuffer(Class<?> recordClass,
            Map<String, Object> fieldValues) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Object[] values = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            String name = components[i].getName();
            if (fieldValues.containsKey(name)) {
                values[i] = fieldValues.get(name);
            } else {
                values[i] = null;
            }
        }

        return new RecordBinder.MutableRecordBuffer(recordClass, recordClass.getSimpleName(),
                components, values);
    }
}
