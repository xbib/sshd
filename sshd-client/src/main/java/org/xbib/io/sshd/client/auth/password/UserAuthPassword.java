package org.xbib.io.sshd.client.auth.password;

import org.xbib.io.sshd.client.auth.AbstractUserAuth;
import org.xbib.io.sshd.client.auth.keyboard.UserInteraction;
import org.xbib.io.sshd.client.session.ClientSession;
import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshConstants;
import org.xbib.io.sshd.common.io.IoWriteFuture;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.buffer.Buffer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

/**
 * Implements the &quot;password&quot; authentication mechanism.
 */
public class UserAuthPassword extends AbstractUserAuth {
    public static final String NAME = UserAuthPasswordFactory.NAME;

    private Iterator<String> passwords;
    private String current;

    public UserAuthPassword() {
        super(NAME);
    }

    @Override
    public void init(ClientSession session, String service) throws Exception {
        super.init(session, service);
        passwords = PasswordIdentityProvider.iteratorOf(session);
    }

    @Override
    protected boolean sendAuthDataRequest(ClientSession session, String service) throws Exception {
        if ((passwords == null) || (!passwords.hasNext())) {
            return false;
        }

        current = passwords.next();
        String username = session.getUsername();
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST,
                username.length() + service.length() + getName().length() + current.length() + Integer.SIZE);
        sendPassword(buffer, session, current, current);
        return true;
    }

    @Override
    protected boolean processAuthDataRequest(ClientSession session, String service, Buffer buffer) throws Exception {
        int cmd = buffer.getUByte();
        if (cmd != SshConstants.SSH_MSG_USERAUTH_PASSWD_CHANGEREQ) {
            throw new IllegalStateException("processAuthDataRequest(" + session + ")[" + service + "]"
                    + " received unknown packet: cmd=" + SshConstants.getCommandMessageName(cmd));
        }

        String prompt = buffer.getString();
        String lang = buffer.getString();
        UserInteraction ui = session.getUserInteraction();
        boolean interactive;
        String password;
        try {
            interactive = (ui != null) && ui.isInteractionAllowed(session);
            password = interactive ? ui.getUpdatedPassword(session, prompt, lang) : null;
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }

        if (interactive) {
            if (GenericUtils.isEmpty(password)) {
                return false;
            } else {
                sendPassword(buffer, session, password, password);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends the password via a {@code SSH_MSG_USERAUTH_REQUEST} message.
     * If old and new password are not the same then it requests a password
     * modification from the server (which may be denied if the server does
     * not support this feature).
     *
     * @param buffer      The {@link Buffer} to re-use for sending the message
     * @param session     The target {@link ClientSession}
     * @param oldPassword The previous password
     * @param newPassword The new password
     * @return An {@link IoWriteFuture} that can be used to wait and check
     * on the success/failure of the request packet being sent
     * @throws IOException If failed to send the message.
     */
    protected IoWriteFuture sendPassword(Buffer buffer, ClientSession session, String oldPassword, String newPassword) throws IOException {
        String username = session.getUsername();
        String service = getService();
        String name = getName();
        boolean modified = !Objects.equals(oldPassword, newPassword);
        buffer = session.createBuffer(SshConstants.SSH_MSG_USERAUTH_REQUEST,
                GenericUtils.length(username) + GenericUtils.length(service)
                        + GenericUtils.length(name) + GenericUtils.length(oldPassword)
                        + (modified ? GenericUtils.length(newPassword) : 0) + Long.SIZE);
        buffer.putString(username);
        buffer.putString(service);
        buffer.putString(name);
        buffer.putBoolean(modified);
        // see RFC-4252 section 8
        buffer.putString(oldPassword);
        if (modified) {
            buffer.putString(newPassword);
        }
        return session.writePacket(buffer);
    }
}
