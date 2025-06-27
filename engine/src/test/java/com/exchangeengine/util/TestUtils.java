package com.exchangeengine.util;

import java.lang.reflect.Field;

/**
 * Utility class for tests
 */
public class TestUtils {
    
    /**
     * Set a private field value using reflection
     * 
     * @param target The object instance to modify
     * @param fieldName The name of the field to modify
     * @param value The new value for the field
     */
    public static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            } else {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found in " + target.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error setting field '" + fieldName + "': " + e.getMessage(), e);
        }
    }
    
    /**
     * Find a field in a class or its superclasses
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }
} 