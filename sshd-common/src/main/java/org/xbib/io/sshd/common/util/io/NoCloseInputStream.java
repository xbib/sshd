package org.xbib.io.sshd.common.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class NoCloseInputStream extends FilterInputStream {
    public NoCloseInputStream(InputStream in) {
        super(in);
    }

    public static InputStream resolveInputStream(InputStream input, boolean okToClose) {
        if ((input == null) || okToClose) {
            return input;
        } else {
            return new NoCloseInputStream(input);
        }
    }

    @Override
    public void close() throws IOException {
        // ignored
    }
}
