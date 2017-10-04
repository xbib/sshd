package org.xbib.io.sshd.common.util.io;

import java.io.InputStream;
import java.nio.channels.Channel;

/**
 *
 */
public abstract class InputStreamWithChannel extends InputStream implements Channel {
    protected InputStreamWithChannel() {
        super();
    }
}
