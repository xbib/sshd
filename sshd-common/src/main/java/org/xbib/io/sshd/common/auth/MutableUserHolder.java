package org.xbib.io.sshd.common.auth;

/**
 *
 */
public interface MutableUserHolder extends UsernameHolder {
    void setUsername(String username);
}
