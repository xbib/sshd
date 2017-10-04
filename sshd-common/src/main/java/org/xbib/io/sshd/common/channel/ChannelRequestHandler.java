package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.util.Transformer;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.function.Function;

/**
 *
 */
public interface ChannelRequestHandler extends RequestHandler<Channel> {

    // required because of generics issues
    Function<ChannelRequestHandler, RequestHandler<Channel>> CHANN2HNDLR = Transformer.identity();

    @Override
    Result process(Channel channel, String request, boolean wantReply, Buffer buffer) throws Exception;

}
