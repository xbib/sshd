package org.xbib.io.sshd.common.x11;

import org.xbib.io.sshd.common.Closeable;
import org.xbib.io.sshd.common.FactoryManager;
import org.xbib.io.sshd.common.io.IoHandler;
import org.xbib.io.sshd.common.util.net.SshdSocketAddress;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface X11ForwardSupport extends Closeable, IoHandler {
    /**
     * Configuration value on the {@link FactoryManager} to control the
     * channel open timeout. If not specified then DEFAULT_CHANNEL_OPEN_TIMEOUT
     * value is used
     */
    String CHANNEL_OPEN_TIMEOUT_PROP = "x11-fwd-open-timeout";
    long DEFAULT_CHANNEL_OPEN_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);

    /**
     * Configuration value to control from which X11 display number to start
     * looking for a free value. If not specified, then {@value #DEFAULT_X11_DISPLAY_OFFSET}
     * is used
     */
    String X11_DISPLAY_OFFSET = "x11-fwd-display-offset";
    int DEFAULT_X11_DISPLAY_OFFSET = 10;

    /**
     * Configuration value to control up to which (but not including) X11 display number
     * to look or a free value. If not specified, then {@value #DEFAULT_X11_MAX_DISPLAYS}
     * is used
     */
    String X11_MAX_DISPLAYS = "x11-fwd-max-display";
    int DEFAULT_X11_MAX_DISPLAYS = 1000;

    /**
     * Configuration value to control the base port number for the X11 display
     * number socket binding. If not specified then {@value #DEFAULT_X11_BASE_PORT}
     * value is used
     */
    String X11_BASE_PORT = "x11-fwd-base-port";
    int DEFAULT_X11_BASE_PORT = 6000;

    /**
     * Configuration value to control the host used to bind to for the X11 display
     * when looking for a free port. If not specified, then {@value #DEFAULT_X11_BIND_HOST}
     * is used
     */
    String X11_BIND_HOST = "x11-fwd-bind-host";
    String DEFAULT_X11_BIND_HOST = SshdSocketAddress.LOCALHOST_IP;

    /**
     * Key for the user DISPLAY variable
     */
    String ENV_DISPLAY = "DISPLAY";

    /**
     * &quot;xauth&quot; command name
     */
    String XAUTH_COMMAND = System.getProperty("sshd.XAUTH_COMMAND", "xauth");

    String createDisplay(
            boolean singleConnection, String authenticationProtocol, String authenticationCookie, int screen)
            throws IOException;
}
