package org.xbib.io.sshd.common.util.io;

import java.io.OutputStream;
import java.nio.channels.Channel;

/**
 *
 */
public abstract class OutputStreamWithChannel extends OutputStream implements Channel {
    protected OutputStreamWithChannel() {
        super();
    }
}
