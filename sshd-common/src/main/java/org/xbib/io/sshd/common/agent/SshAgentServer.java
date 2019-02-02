package org.xbib.io.sshd.common.agent;

import java.nio.channels.Channel;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface SshAgentServer extends Channel {

    long DEFAULT_CHANNEL_OPEN_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);

    /**
     * Value used to configure the type of proxy forwarding channel to be
     * used. If not specified, then {@link #DEFAULT_PROXY_CHANNEL_TYPE}
     * is used
     */
    String PROXY_CHANNEL_TYPE = "ssh-agent-server-channel-proxy-type";
    // see also https://tools.ietf.org/html/draft-ietf-secsh-agent-02
    String DEFAULT_PROXY_CHANNEL_TYPE = "auth-agent@openssh.com";

    /**
     * @return Agent server identifier
     */
    String getId();

}
