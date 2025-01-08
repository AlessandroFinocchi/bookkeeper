package org.apache.bookkeeper.bookie;

import java.lang.reflect.Field;

public class ReflectionUtils {
    /**
     * Metodo per accedere a un campo privato di un oggetto usando Reflection.
     * Usage:
     *  # MyClass has a private integer field named x, initialized in constructor
     *  MyClass c = new MyClass(5);
     *  int a = getPrivateFieldValue(c, "x", Integer.class);
     *
     * @param targetObject L'oggetto che contiene il campo.
     * @param fieldName Il nome del campo privato da accedere.
     * @param fieldType Il tipo del campo (esempio: String.class).
     * @param <T> Il tipo generico del valore restituito.
     * @return Il valore del campo specificato.
     * @throws NoSuchFieldException Se il campo non esiste.
     * @throws IllegalAccessException Se l'accesso al campo non è consentito.
     */
    public static <T> T getPrivateFieldValue(Object targetObject, String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {

        // Ottieni la classe dell'oggetto target
        Class<?> targetClass = targetObject.getClass();

        // Ottieni il campo specificato
        Field field = targetClass.getDeclaredField(fieldName);

        // Rendi il campo accessibile
        field.setAccessible(true);

        // Leggi il valore del campo e castalo al tipo specificato
        return fieldType.cast(field.get(targetObject));
    }
}
