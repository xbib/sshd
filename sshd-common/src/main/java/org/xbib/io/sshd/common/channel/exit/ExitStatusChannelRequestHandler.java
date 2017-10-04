package org.xbib.io.sshd.common.channel.exit;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.util.EventNotifier;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class ExitStatusChannelRequestHandler extends AbstractChannelExitRequestHandler<Integer> {
    public static final String NAME = "exit-status";

    public ExitStatusChannelRequestHandler(AtomicReference<Integer> holder, EventNotifier<? super String> notifier) {
        super(holder, notifier);
    }

    @Override
    public final String getName() {
        return NAME;
    }

    @Override
    protected Integer processRequestValue(Channel channel, String request, Buffer buffer) throws Exception {
        return processRequestValue(channel, buffer.getInt());
    }

    protected Integer processRequestValue(Channel channel, int exitStatus) throws Exception {

        return exitStatus;
    }
}
