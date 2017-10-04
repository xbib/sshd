package org.xbib.io.sshd.common.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an un-modifiable pair of values.
 *
 * @param <F> First value type
 * @param <S> Second value type
 */
public class Pair<F, S> implements Map.Entry<F, S> {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Comparator<Map.Entry<Comparable, ?>> BY_KEY_COMPARATOR = (o1, o2) -> {
        Comparable k1 = o1.getKey();
        Comparable k2 = o2.getKey();
        return k1.compareTo(k2);
    };

    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @param <K> The {@link Comparable} key type
     * @param <V> The associated entry value
     * @return A {@link Comparator} for {@link Map.Entry}-ies that
     * compares the key values
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K extends Comparable<K>, V> Comparator<Map.Entry<K, V>> byKeyEntryComparator() {
        return (Comparator) BY_KEY_COMPARATOR;
    }

    @Override
    public final F getKey() {
        return getFirst();
    }

    @Override
    public S getValue() {
        return getSecond();
    }

    @Override
    public S setValue(S value) {
        throw new UnsupportedOperationException("setValue(" + value + ") N/A");
    }

    public final F getFirst() {
        return first;
    }

    public final S getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFirst()) * 31 + Objects.hashCode(getSecond());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Pair<?, ?> other = (Pair<?, ?>) obj;
        return Objects.equals(getFirst(), other.getFirst()) && Objects.equals(getSecond(), other.getSecond());
    }

    @Override
    public String toString() {
        return Objects.toString(getFirst()) + ", " + Objects.toString(getSecond());
    }
}