package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.server.session.ServerSessionHolder;

/**
 *
 */
public interface ServerChannel extends Channel, ServerSessionHolder {
    // Marker interface
}
