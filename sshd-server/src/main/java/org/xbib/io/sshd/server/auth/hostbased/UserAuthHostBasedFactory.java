package org.xbib.io.sshd.server.auth.hostbased;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.server.auth.AbstractUserAuthFactory;

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
    };

    private List<NamedFactory<Signature>> factories;

    public UserAuthHostBasedFactory() {
        this(null);
    }

    public UserAuthHostBasedFactory(List<NamedFactory<Signature>> factories) {
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
    public UserAuthHostBased create() {
        return new UserAuthHostBased(getSignatureFactories());
    }
}