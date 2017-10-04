package org.xbib.io.sshd.common.channel.exit;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.util.EventNotifier;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class ExitSignalChannelRequestHandler extends AbstractChannelExitRequestHandler<String> {
    public static final String NAME = "exit-signal";

    public ExitSignalChannelRequestHandler(AtomicReference<String> holder, EventNotifier<? super String> notifier) {
        super(holder, notifier);
    }

    @Override
    public final String getName() {
        return NAME;
    }

    @Override
    protected String processRequestValue(Channel channel, String request, Buffer buffer) throws Exception {
        return processRequestValue(channel, buffer.getString(), buffer.getBoolean(), buffer.getString(), buffer.getString());
    }

    protected String processRequestValue(Channel channel, String signalName, boolean coreDumped, String message, String lang) throws Exception {
        return signalName;
    }
}
