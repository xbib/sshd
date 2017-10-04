package org.xbib.io.sshd.common.util.net;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class NetworkConnector extends AbstractLoggingBean {
    public static final String DEFAULT_HOST = SshdSocketAddress.LOCALHOST_IP;
    public static final long DEFAULT_CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);
    public static final long DEFAULT_READ_TIMEOUT = TimeUnit.SECONDS.toMillis(15L);

    private String protocol;
    private String host = DEFAULT_HOST;
    private int port;
    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private long readTimeout = DEFAULT_READ_TIMEOUT;

    public NetworkConnector() {
        super();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public String toString() {
        return getProtocol() + "://" + getHost() + ":" + getPort()
                + ";connect=" + getConnectTimeout()
                + ";read=" + getReadTimeout();
    }
}
