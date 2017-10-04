package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.util.buffer.Buffer;
import org.xbib.io.sshd.server.auth.AbstractUserAuth;
import org.xbib.io.sshd.server.session.ServerSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Issue a &quot;keyboard-interactive&quot; command according to <A HREF="https://www.ietf.org/rfc/rfc4256.txt">RFC4256</A>.
 */
public class UserAuthKeyboardInteractive extends AbstractUserAuth {
    public static final String NAME = UserAuthKeyboardInteractiveFactory.NAME;

    public UserAuthKeyboardInteractive() {
        super(NAME);
    }

    @Override
    protected Boolean doAuth(Buffer buffer, boolean init) throws Exception {
        ServerSession session = getServerSession();
        String username = getUsername();
        KeyboardInteractiveAuthenticator auth = session.getKeyboardInteractiveAuthenticator();
        if (init) {
            String lang = buffer.getString();
            String subMethods = buffer.getString();
            if (auth == null) {
                return false;
            }

            InteractiveChallenge challenge;
            try {
                challenge = auth.generateChallenge(session, username, lang, subMethods);
            } catch (Error e) {
                throw new RuntimeSshException(e);
            }

            if (challenge == null) {
                return false;
            }

            // Prompt for password
            buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_INFO_REQUEST);
            challenge.append(buffer);
            session.writePacket(buffer);
            return null;
        } else {
            int cmd = buffer.getUByte();
            if (cmd != SshConstants.SSH_MSG_USERAUTH_INFO_RESPONSE) {
                throw new SshException("Received unexpected message: " + SshConstants.getCommandMessageName(cmd));
            }

            int num = buffer.getInt();
            List<String> responses = (num <= 0) ? Collections.emptyList() : new ArrayList<>(num);
            for (int index = 0; index < num; index++) {
                String value = buffer.getString();
                responses.add(value);
            }

            if (auth == null) {
                return false;
            }

            boolean authed;
            try {
                authed = auth.authenticate(session, username, responses);
            } catch (Error e) {
                throw new RuntimeSshException(e);
            }
            return authed;
        }
    }
}
