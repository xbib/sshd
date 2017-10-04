package org.xbib.io.sshd.common.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class NoCloseOutputStream extends FilterOutputStream {
    public NoCloseOutputStream(OutputStream out) {
        super(out);
    }

    public static OutputStream resolveOutputStream(OutputStream output, boolean okToClose) {
        if ((output == null) || okToClose) {
            return output;
        } else {
            return new NoCloseOutputStream(output);
        }
    }

    @Override
    public void close() throws IOException {
        // ignored
    }
}
