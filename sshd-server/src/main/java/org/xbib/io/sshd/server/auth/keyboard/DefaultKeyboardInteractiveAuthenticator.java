package org.xbib.io.sshd.server.auth.keyboard;

import org.xbib.io.sshd.common.RuntimeSshException;
import org.xbib.io.sshd.common.SshException;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.auth.password.PasswordAuthenticator;
import org.xbib.io.sshd.server.session.ServerSession;

import java.util.List;

/**
 * Provides a default implementation for {@link KeyboardInteractiveAuthenticator}
 * where it prompts for the password.
 */
public class DefaultKeyboardInteractiveAuthenticator
        extends AbstractLoggingBean
        implements KeyboardInteractiveAuthenticator {
    // configuration parameters on the FactoryManager to configure the message values
    public static final String KB_INTERACTIVE_NAME_PROP = "kb-server-interactive-name";
    public static final String DEFAULT_KB_INTERACTIVE_NAME = "Password authentication";
    public static final String KB_INTERACTIVE_INSTRUCTION_PROP = "kb-server-interactive-instruction";
    public static final String DEFAULT_KB_INTERACTIVE_INSTRUCTION = "";
    public static final String KB_INTERACTIVE_LANG_PROP = "kb-server-interactive-language";
    public static final String DEFAULT_KB_INTERACTIVE_LANG = "en-US";
    public static final String KB_INTERACTIVE_PROMPT_PROP = "kb-server-interactive-prompt";
    public static final String DEFAULT_KB_INTERACTIVE_PROMPT = "Password: ";
    public static final String KB_INTERACTIVE_ECHO_PROMPT_PROP = "kb-server-interactive-echo-prompt";
    public static final boolean DEFAULT_KB_INTERACTIVE_ECHO_PROMPT = false;

    public static final DefaultKeyboardInteractiveAuthenticator INSTANCE = new DefaultKeyboardInteractiveAuthenticator();

    public DefaultKeyboardInteractiveAuthenticator() {
        super();
    }

    @Override
    public InteractiveChallenge generateChallenge(ServerSession session, String username, String lang, String subMethods) {
        PasswordAuthenticator auth = session.getPasswordAuthenticator();
        if (auth == null) {
            return null;
        }

        InteractiveChallenge challenge = new InteractiveChallenge();
        challenge.setInteractionName(getInteractionName(session));
        challenge.setInteractionInstruction(getInteractionInstruction(session));
        challenge.setLanguageTag(getInteractionLanguage(session));
        challenge.addPrompt(getInteractionPrompt(session), isInteractionPromptEchoEnabled(session));
        return challenge;
    }

    @Override
    public boolean authenticate(ServerSession session, String username, List<String> responses) throws Exception {
        PasswordAuthenticator auth = session.getPasswordAuthenticator();
        if (auth == null) {
            return false;
        }

        int numResp = GenericUtils.size(responses);
        if (numResp != 1) {
            throw new SshException("Mismatched number of responses");
        }

        try {
            return auth.authenticate(username, responses.get(0), session);
        } catch (Error e) {
            throw new RuntimeSshException(e);
        }
    }

    protected String getInteractionName(ServerSession session) {
        return session.getStringProperty(KB_INTERACTIVE_NAME_PROP, DEFAULT_KB_INTERACTIVE_NAME);
    }

    protected String getInteractionInstruction(ServerSession session) {
        return session.getStringProperty(KB_INTERACTIVE_INSTRUCTION_PROP, DEFAULT_KB_INTERACTIVE_INSTRUCTION);
    }

    protected String getInteractionLanguage(ServerSession session) {
        return session.getStringProperty(KB_INTERACTIVE_LANG_PROP, DEFAULT_KB_INTERACTIVE_LANG);
    }

    protected String getInteractionPrompt(ServerSession session) {
        return session.getStringProperty(KB_INTERACTIVE_PROMPT_PROP, DEFAULT_KB_INTERACTIVE_PROMPT);
    }

    protected boolean isInteractionPromptEchoEnabled(ServerSession session) {
        return session.getBooleanProperty(KB_INTERACTIVE_ECHO_PROMPT_PROP, DEFAULT_KB_INTERACTIVE_ECHO_PROMPT);
    }
}
