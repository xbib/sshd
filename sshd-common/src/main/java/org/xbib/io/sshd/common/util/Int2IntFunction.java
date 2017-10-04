package org.xbib.io.sshd.common.util;

import java.util.function.IntUnaryOperator;

/**
 *
 */
public final class Int2IntFunction {
    private Int2IntFunction() {
        throw new UnsupportedOperationException("No instance");
    }

    public static IntUnaryOperator sub(int delta) {
        return add(0 - delta);
    }

    public static IntUnaryOperator add(int delta) {
        if (delta == 0) {
            return IntUnaryOperator.identity();
        } else {
            return value -> value + delta;
        }
    }

    public static IntUnaryOperator mul(int factor) {
        if (factor == 0) {
            return constant(0);
        } else if (factor == 1) {
            return IntUnaryOperator.identity();
        } else {
            return value -> value * factor;
        }
    }

    public static IntUnaryOperator constant(int v) {
        return value -> v;
    }

    public static IntUnaryOperator div(int factor) {
        if (factor == 1) {
            return IntUnaryOperator.identity();
        } else {
            ValidateUtils.checkTrue(factor != 0, "Zero division factor");
            return value -> value / factor;
        }
    }
}
