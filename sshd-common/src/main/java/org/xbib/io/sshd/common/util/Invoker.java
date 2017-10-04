package org.xbib.io.sshd.common.util;

import java.util.Collection;

/**
 * The complement to the {@code Callable} interface - accepts one argument
 * and possibly throws somethind
 *
 * @param <ARG> Argument type
 * @param <RET> Return type
 */
@FunctionalInterface
public interface Invoker<ARG, RET> {
    static <ARG> Invoker<ARG, Void> wrapAll(Collection<? extends Invoker<? super ARG, ?>> invokers) {
        return arg -> {
            invokeAll(arg, invokers);
            return null;
        };
    }

    /**
     * Invokes <U>all</U> the instances ignoring the return value. Any
     * intermediate exceptions are accumulated and thrown at the end.
     *
     * @param <ARG>    Argument type
     * @param arg      The argument to pass to the {@link #invoke(Object)} method
     * @param invokers The invokers to scan - ignored if {@code null}/empty
     *                 (also ignores {@code null} members)
     * @throws Throwable If invocation failed
     */
    static <ARG> void invokeAll(ARG arg, Collection<? extends Invoker<? super ARG, ?>> invokers) throws Throwable {
        if (GenericUtils.isEmpty(invokers)) {
            return;
        }

        Throwable err = null;
        for (Invoker<? super ARG, ?> i : invokers) {
            if (i == null) {
                continue;
            }

            try {
                i.invoke(arg);
            } catch (Throwable t) {
                err = GenericUtils.accumulateException(err, t);
            }
        }

        if (err != null) {
            throw err;
        }
    }

    static <ARG> Invoker<ARG, Void> wrapFirst(Collection<? extends Invoker<? super ARG, ?>> invokers) {
        return arg -> {
            Pair<Invoker<? super ARG, ?>, Throwable> result = invokeTillFirstFailure(arg, invokers);
            if (result != null) {
                throw result.getValue();
            }
            return null;
        };
    }

    /**
     * Invokes all instances until 1st failure (if any)
     *
     * @param <ARG>    Argument type
     * @param arg      The argument to pass to the {@link #invoke(Object)} method
     * @param invokers The invokers to scan - ignored if {@code null}/empty
     *                 (also ignores {@code null} members)
     * @return A {@link Pair} representing the <U>first</U> failed invocation
     * - {@code null} if all were successful (or none invoked).
     */
    static <ARG> Pair<Invoker<? super ARG, ?>, Throwable> invokeTillFirstFailure(ARG arg, Collection<? extends Invoker<? super ARG, ?>> invokers) {
        if (GenericUtils.isEmpty(invokers)) {
            return null;
        }

        for (Invoker<? super ARG, ?> i : invokers) {
            if (i == null) {
                continue;
            }

            try {
                i.invoke(arg);
            } catch (Throwable t) {
                return new Pair<>(i, t);
            }
        }

        return null;
    }

    RET invoke(ARG arg) throws Throwable;
}
