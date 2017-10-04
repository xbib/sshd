package org.xbib.io.sshd.common.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 */
public final class ReflectionUtils {
    public static final Function<Field, String> FIELD_NAME_EXTRACTOR = f -> (f == null) ? null : f.getName();

    private ReflectionUtils() {
        throw new UnsupportedOperationException("No instance");
    }

    public static Collection<Field> getMatchingFields(Class<?> clazz, Predicate<? super Field> acceptor) {
        return org.xbib.io.sshd.common.util.GenericUtils.selectMatchingMembers(acceptor, clazz.getFields());
    }

    public static Collection<Field> getMatchingDeclaredFields(Class<?> clazz, Predicate<? super Field> acceptor) {
        return GenericUtils.selectMatchingMembers(acceptor, clazz.getDeclaredFields());
    }

    public static boolean isClassAvailable(ClassLoader cl, String className) {
        try {
            cl.loadClass(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
