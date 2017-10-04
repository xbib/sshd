package org.xbib.io.sshd.client.session;

/**
 */
public interface ClientProxyConnectorHolder {
    ClientProxyConnector getClientProxyConnector();

    void setClientProxyConnector(ClientProxyConnector proxyConnector);
}
