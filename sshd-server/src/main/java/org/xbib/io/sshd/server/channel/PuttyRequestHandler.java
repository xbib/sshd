package org.xbib.io.sshd.server.channel;

import org.xbib.io.sshd.common.channel.AbstractChannelRequestHandler;
import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.channel.PtyMode;
import org.xbib.io.sshd.common.session.Session;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles Putty specific channel requests as indicated by
 * <A HREF="http://tartarus.org/~simon/putty-snapshots/htmldoc/AppendixF.html">Appendix F: SSH-2 names specified for PuTTY</A>.
 */
public class PuttyRequestHandler extends AbstractChannelRequestHandler {
    /**
     * Suffix of all PUTTY related channel requests
     */
    public static final String REQUEST_SUFFIX = "@putty.projects.tartarus.org";

    public static final Set<PtyMode> PUTTY_OPTIONS =
            Collections.unmodifiableSet(EnumSet.of(PtyMode.ECHO, PtyMode.ICRNL, PtyMode.ONLCR));

    public static final PuttyRequestHandler INSTANCE = new PuttyRequestHandler();

    public PuttyRequestHandler() {
        super();
    }

    /**
     * @param request The channel request value - ignored if {@code null}/empty
     * @return {@code true} if the request ends in {@link #REQUEST_SUFFIX}
     */
    public static boolean isPuttyRequest(String request) {
        return (GenericUtils.length(request) > REQUEST_SUFFIX.length()) && request.endsWith(REQUEST_SUFFIX);
    }

    /**
     * @param session The current {@link Session} - ignored if {@code null}
     * @return {@code true} if it is a PUTTY session
     * @see Session#getClientVersion()
     * @see #isPuttyClient(String)
     */
    public static boolean isPuttyClient(Session session) {
        return isPuttyClient((session == null) ? null : session.getClientVersion());
    }

    /**
     * @param clientVersion The client identification string - ignored if {@code null}/empty
     * @return {@code true} if the identification starts with the {@link Session#DEFAULT_SSH_VERSION_PREFIX}
     * and it contains the &quot;putty&quot; string (case insensitive)
     */
    public static boolean isPuttyClient(String clientVersion) {
        return (GenericUtils.length(clientVersion) > Session.DEFAULT_SSH_VERSION_PREFIX.length())
                && clientVersion.startsWith(Session.DEFAULT_SSH_VERSION_PREFIX)
                && clientVersion.toLowerCase().contains("putty");
    }

    public static Map<PtyMode, Integer> resolveShellTtyOptions(Map<PtyMode, Integer> modes) {
        Map<PtyMode, Integer> resolved = PtyMode.createEnabledOptions(PUTTY_OPTIONS);
        if (GenericUtils.size(modes) > 0) {
            resolved.putAll(modes); // TODO consider adding only non-overriding options
        }
        return resolved;
    }

    @Override
    public Result process(Channel channel, String request, boolean wantReply, Buffer buffer) throws Exception {
        if (!isPuttyRequest(request)) {
            return Result.Unsupported;
        }

        String opcode = request.substring(0, request.length() - REQUEST_SUFFIX.length());
        return processPuttyOpcode(channel, request, opcode, wantReply, buffer);
    }

    protected Result processPuttyOpcode(Channel channel, String request, String opcode, boolean wantReply, Buffer buffer) throws Exception {
        if ("simple".equalsIgnoreCase(opcode)) {
            // Quote: "There is no message-specific data"
            return Result.ReplySuccess;
        } else if ("winadj".equalsIgnoreCase(opcode)) {
            // Quote: "Servers MUST treat it as an unrecognized request and respond with SSH_MSG_CHANNEL_FAILURE"
            return Result.ReplyFailure;
        }
        return Result.ReplyFailure;
    }
}
