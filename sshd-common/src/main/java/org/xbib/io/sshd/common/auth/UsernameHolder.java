package org.xbib.io.sshd.common.auth;

/**
 */
@FunctionalInterface
public interface UsernameHolder {
    /**
     * @return The attached username - may be {@code null}/empty if holder
     * not yet initialized
     */
    String getUsername();
}
