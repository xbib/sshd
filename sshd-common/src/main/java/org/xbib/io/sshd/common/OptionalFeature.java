package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collection;

/**
 *
 */
@FunctionalInterface
public interface OptionalFeature {
    OptionalFeature TRUE = new OptionalFeature() {
        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public String toString() {
            return "TRUE";
        }
    };

    OptionalFeature FALSE = new OptionalFeature() {
        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public String toString() {
            return "FALSE";
        }
    };

    static OptionalFeature of(boolean supported) {
        return supported ? TRUE : FALSE;
    }

    static OptionalFeature all(Collection<? extends OptionalFeature> features) {
        return () -> {
            if (GenericUtils.isEmpty(features)) {
                return false;
            }

            for (OptionalFeature f : features) {
                if (!f.isSupported()) {
                    return false;
                }
            }

            return true;
        };
    }

    static OptionalFeature any(Collection<? extends OptionalFeature> features) {
        return () -> {
            if (GenericUtils.isEmpty(features)) {
                return false;
            }

            for (OptionalFeature f : features) {
                if (f.isSupported()) {
                    return true;
                }
            }

            return false;
        };
    }

    boolean isSupported();
}
