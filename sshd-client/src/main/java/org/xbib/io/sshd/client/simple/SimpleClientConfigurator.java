package org.xbib.io.sshd.client.simple;

import org.xbib.io.sshd.common.config.SshConfigFileReader;

/**
 */
public interface SimpleClientConfigurator {
    /**
     * Default connect timeout (msec.) unless {@link #setConnectTimeout(long)} is used
     */
    long DEFAULT_CONNECT_TIMEOUT = Long.MAX_VALUE;  // virtually infinite

    /**
     * Default authentication timeout (msec.) unless {@link #setAuthenticationTimeout(long)} is used
     */
    long DEFAULT_AUTHENTICATION_TIMEOUT = Long.MAX_VALUE;   // virtually infinite

    int DEFAULT_PORT = SshConfigFileReader.DEFAULT_PORT;

    /**
     * @return Current connect timeout (msec.) - always positive
     */
    long getConnectTimeout();

    /**
     * @param timeout Requested connect timeout (msec.) - always positive
     */
    void setConnectTimeout(long timeout);

    /**
     * @return Current authentication timeout (msec.) - always positive
     */
    long getAuthenticationTimeout();

    /**
     * @param timeout Requested authentication timeout (msec.) - always positive
     */
    void setAuthenticationTimeout(long timeout);
}
