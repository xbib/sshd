package org.xbib.io.sshd.server.session;

/**
 *
 */
public interface ServerProxyAcceptorHolder {
    ServerProxyAcceptor getServerProxyAcceptor();

    void setServerProxyAcceptor(ServerProxyAcceptor proxyAcceptor);
}
