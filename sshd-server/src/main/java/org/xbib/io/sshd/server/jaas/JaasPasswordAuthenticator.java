package org.xbib.io.sshd.server.jaas;

import org.xbib.io.sshd.common.util.logging.AbstractLoggingBean;
import org.xbib.io.sshd.server.auth.password.PasswordAuthenticator;
import org.xbib.io.sshd.server.session.ServerSession;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

/**
 *
 */
public class JaasPasswordAuthenticator extends AbstractLoggingBean implements PasswordAuthenticator {

    private String domain;

    public JaasPasswordAuthenticator() {
        this(null);
    }

    public JaasPasswordAuthenticator(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        return authenticate(username, password);
    }

    public boolean authenticate(final String username, final String password) {
        try {
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(domain, subject, callbacks -> {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback);
                    }
                }
            });
            loginContext.login();
            loginContext.logout();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
