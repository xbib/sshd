package org.xbib.io.sshd.server.auth.password;

/**
 * A special exception that can be thrown by the {@link PasswordAuthenticator}
 * to indicate that the password requires changing or is not string enough.
 */
public class PasswordChangeRequiredException extends RuntimeException {
    private static final long serialVersionUID = -8522928326608137895L;
    private final String prompt;
    private final String lang;

    public PasswordChangeRequiredException(String message, String prompt, String lang) {
        this(message, prompt, lang, null);
    }

    public PasswordChangeRequiredException(Throwable cause, String prompt, String lang) {
        this(cause.getMessage(), prompt, lang, cause);
    }

    public PasswordChangeRequiredException(String message, String prompt, String lang, Throwable cause) {
        super(message, cause);
        this.prompt = prompt;
        this.lang = lang;
    }

    /**
     * @return The prompt to show to the user - may be {@code null}/empty
     */
    public final String getPrompt() {
        return prompt;
    }

    /**
     * @return The language code for the prompt - may be {@code null}/empty
     */
    public final String getLanguage() {
        return lang;
    }
}
