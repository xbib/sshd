package org.xbib.io.sshd.common.channel;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A global request handler.
 *
 * @param <T> Request type
 */
@FunctionalInterface
public interface RequestHandler<T> {

    /**
     * Process an SSH request. If an exception is thrown, the ConnectionService
     * will send a failure message if needed and the request will be considered handled.
     *
     * @param t         The input parameter
     * @param request   The request string
     * @param wantReply Whether a reply is requested
     * @param buffer    The {@link Buffer} with request specific data
     * @return The {@link Result}
     * @throws Exception If failed to handle the request - <B>Note:</B> in
     *                   order to signal an unsupported request the {@link Result#Unsupported}
     *                   value should be returned
     */
    Result process(T t, String request, boolean wantReply, Buffer buffer) throws Exception;

    enum Result {
        Unsupported,
        Replied,
        ReplySuccess,
        ReplyFailure;

        public static final Set<Result> VALUES =
                Collections.unmodifiableSet(EnumSet.allOf(Result.class));

        /**
         * @param name The result name - ignored if {@code null}/empty
         * @return The matching {@link Result} value (case <U>insensitive</U>)
         * or {@code null} if no match found
         */
        public static Result fromName(String name) {
            if (GenericUtils.isEmpty(name)) {
                return null;
            }

            for (Result r : VALUES) {
                if (name.equalsIgnoreCase(r.name())) {
                    return r;
                }
            }

            return null;
        }
    }
}
