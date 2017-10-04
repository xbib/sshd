package org.xbib.io.sshd.common.util.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 */
public class NoCloseReader extends FilterReader {
    public NoCloseReader(Reader in) {
        super(in);
    }

    public static Reader resolveReader(Reader r, boolean okToClose) {
        if ((r == null) || okToClose) {
            return r;
        } else {
            return new NoCloseReader(r);
        }
    }

    @Override
    public void close() throws IOException {
        // ignored
    }
}
