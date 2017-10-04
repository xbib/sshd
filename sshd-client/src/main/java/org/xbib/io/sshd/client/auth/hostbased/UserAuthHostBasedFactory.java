package org.xbib.io.sshd.client.auth.hostbased;

import org.xbib.io.sshd.client.auth.AbstractUserAuthFactory;
import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.List;

/**
 *
 */
public class UserAuthHostBasedFactory extends AbstractUserAuthFactory implements SignatureFactoriesManager {
    public static final String NAME = HOST_BASED;
    public static final UserAuthHostBasedFactory INSTANCE = new UserAuthHostBasedFactory() {
        @Override
        public List<NamedFactory<Signature>> getSignatureFactories() {
            return null;
        }

        @Override
        public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
            if (!GenericUtils.isEmpty(factories)) {
                throw new UnsupportedOperationException("Not allowed to change default instance signature factories");
            }
        }

        @Override
        public org.xbib.io.sshd.client.auth.hostbased.HostKeyIdentityProvider getClientHostKeys() {
            return null;
        }

        @Override
        public void setClientHostKeys(org.xbib.io.sshd.client.auth.hostbased.HostKeyIdentityProvider clientHostKeys) {
            if (clientHostKeys != null) {
                throw new UnsupportedOperationException("Not allowed to change default instance client host keys");
            }
        }

        @Override
        public String getClientUsername() {
            return null;
        }

        @Override
        public void setClientUsername(String clientUsername) {
            if (!GenericUtils.isEmpty(clientUsername)) {
                throw new UnsupportedOperationException("Not allowed to change default instance client username");
            }
        }

        @Override
        public String getClientHostname() {
            return null;
        }

        @Override
        public void setClientHostname(String clientHostname) {
            if (!GenericUtils.isEmpty(clientHostname)) {
                throw new UnsupportedOperationException("Not allowed to change default instance client hostname");
            }
        }
    };

    private List<NamedFactory<Signature>> factories;
    private org.xbib.io.sshd.client.auth.hostbased.HostKeyIdentityProvider clientHostKeys;
    private String clientUsername;
    private String clientHostname;

    public UserAuthHostBasedFactory() {
        super(NAME);
    }

    @Override
    public List<NamedFactory<Signature>> getSignatureFactories() {
        return factories;
    }

    @Override
    public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
        this.factories = factories;
    }

    public org.xbib.io.sshd.client.auth.hostbased.HostKeyIdentityProvider getClientHostKeys() {
        return clientHostKeys;
    }

    public void setClientHostKeys(HostKeyIdentityProvider clientHostKeys) {
        this.clientHostKeys = clientHostKeys;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public String getClientHostname() {
        return clientHostname;
    }

    public void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }

    @Override
    public org.xbib.io.sshd.client.auth.hostbased.UserAuthHostBased create() {
        org.xbib.io.sshd.client.auth.hostbased.UserAuthHostBased auth = new UserAuthHostBased(getClientHostKeys());
        auth.setClientHostname(getClientHostname());
        auth.setClientUsername(getClientUsername());
        auth.setSignatureFactories(getSignatureFactories());
        return auth;
    }
}
