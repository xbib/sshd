package org.xbib.io.sshd.server.auth.pubkey;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.server.auth.AbstractUserAuthFactory;

import java.util.List;

/**
 *
 */
public class UserAuthPublicKeyFactory extends AbstractUserAuthFactory implements SignatureFactoriesManager {
    public static final String NAME = PUBLIC_KEY;
    public static final UserAuthPublicKeyFactory INSTANCE = new UserAuthPublicKeyFactory() {
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
    };

    private List<NamedFactory<Signature>> factories;

    public UserAuthPublicKeyFactory() {
        this(null);
    }

    public UserAuthPublicKeyFactory(List<NamedFactory<Signature>> factories) {
        super(NAME);
        this.factories = factories; // OK if null/empty
    }

    @Override
    public List<NamedFactory<Signature>> getSignatureFactories() {
        return factories;
    }

    @Override
    public void setSignatureFactories(List<NamedFactory<Signature>> factories) {
        this.factories = factories;
    }

    @Override
    public org.xbib.io.sshd.server.auth.pubkey.UserAuthPublicKey create() {
        return new UserAuthPublicKey(getSignatureFactories());
    }
}