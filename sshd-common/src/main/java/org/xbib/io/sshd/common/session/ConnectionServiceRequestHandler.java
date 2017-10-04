package org.xbib.io.sshd.common.session;

import org.xbib.io.sshd.common.channel.RequestHandler;
import org.xbib.io.sshd.common.util.Transformer;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.function.Function;

/**
 *
 */
public interface ConnectionServiceRequestHandler extends RequestHandler<ConnectionService> {

    // required because of generics issues
    Function<ConnectionServiceRequestHandler, RequestHandler<ConnectionService>> SVC2HNDLR = Transformer.identity();

    @Override
    Result process(ConnectionService service, String request, boolean wantReply, Buffer buffer) throws Exception;
}
