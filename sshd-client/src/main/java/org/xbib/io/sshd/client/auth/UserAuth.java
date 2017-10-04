package org.xbib.io.sshd.client.auth;

import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.client.session.ClientSessionHolder;
import org.xbib.io.sshd.common.auth.UserAuthInstance;
import org.xbib.io.sshd.common.util.buffer.Buffer;

/**
 * Represents a user authentication mechanism.
 */
public interface UserAuth extends ClientSessionHolder, UserAuthInstance<ClientSession> {

    /**
     * @param session The {@link ClientSession}
     * @param service The requesting service name
     * @throws Exception If failed to initialize the mechanism
     */
    void init(ClientSession session, String service) throws Exception;

    /**
     * @param buffer The {@link Buffer} to process - {@code null} if not a response buffer,
     *               i.e., the underlying authentication mechanism should initiate whatever challenge/response
     *               mechanism is required
     * @return {@code true} if request handled - {@code false} if the next authentication
     * mechanism should be used
     * @throws Exception If failed to process the request
     */
    boolean process(Buffer buffer) throws Exception;

    /**
     * Called to release any allocated resources
     */
    void destroy();

}
