package org.xbib.io.sshd.common.util.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 *
 */
public class NoCloseWriter extends FilterWriter {
    public NoCloseWriter(Writer out) {
        super(out);
    }

    public static Writer resolveWriter(Writer r, boolean okToClose) {
        if ((r == null) || okToClose) {
            return r;
        } else {
            return new NoCloseWriter(r);
        }
    }

    @Override
    public void close() throws IOException {
        // ignored
    }
}
