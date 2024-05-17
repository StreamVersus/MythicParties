package ru.streamversus.mythicparties.database;

import java.util.Set;

/**
 * Database compatibility
 * @param <T> key
 * @param <U> value
 */
public interface dbMap<T, U> {
    void add(T t, U u);
    void update(T t, U u);
    U remove(T t);
    U get(T t);
    Set<T> idSet();
    boolean contains(T t);
    void replace(T t, U u);
}
