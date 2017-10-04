package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.cipher.Cipher;
import org.xbib.io.sshd.common.compression.Compression;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.mac.Mac;
import org.xbib.io.sshd.common.signature.Signature;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.closeable.AbstractInnerCloseable;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public abstract class AbstractKexFactoryManager
        extends AbstractInnerCloseable
        implements KexFactoryManager {
    private KexFactoryManager parent;
    private List<NamedFactory<KeyExchange>> keyExchangeFactories;
    private List<NamedFactory<Cipher>> cipherFactories;
    private List<NamedFactory<Compression>> compressionFactories;
    private List<NamedFactory<Mac>> macFactories;
    private List<NamedFactory<Signature>> signatureFactories;
    private KeyPairProvider keyPairProvider;

    protected AbstractKexFactoryManager() {
        this(null);
    }

    protected AbstractKexFactoryManager(KexFactoryManager parent) {
        this.parent = parent;
    }

    @Override
    public List<NamedFactory<KeyExchange>> getKeyExchangeFactories() {
        return resolveEffectiveFactories(KeyExchange.class, keyExchangeFactories,
                (parent == null) ? Collections.emptyList() : parent.getKeyExchangeFactories());
    }

    @Override
    public void setKeyExchangeFactories(List<NamedFactory<KeyExchange>> keyExchangeFactories) {
        this.keyExchangeFactories = keyExchangeFactories;
    }

    @Override
    public List<NamedFactory<Cipher>> getCipherFactories() {
        return resolveEffectiveFactories(Cipher.class, cipherFactories,
                (parent == null) ? Collections.emptyList() : parent.getCipherFactories());
    }

    @Override
    public void setCipherFactories(List<NamedFactory<Cipher>> cipherFactories) {
        this.cipherFactories = cipherFactories;
    }

    @Override
    public List<NamedFactory<Compression>> getCompressionFactories() {
        return resolveEffectiveFactories(Compression.class, compressionFactories,
                (parent == null) ? Collections.emptyList() : parent.getCompressionFactories());
    }

    @Override
    public void setCompressionFactories(List<NamedFactory<Compression>> compressionFactories) {
        this.compressionFactories = compressionFactories;
    }

    @Override
    public List<NamedFactory<Mac>> getMacFactories() {
        return resolveEffectiveFactories(Mac.class, macFactories,
                (parent == null) ? Collections.emptyList() : parent.getMacFactories());
    }

    @Override
    public void setMacFactories(List<NamedFactory<Mac>> macFactories) {
        this.macFactories = macFactories;
    }

    @Override
    public List<NamedFactory<Signature>> getSignatureFactories() {
        return resolveEffectiveFactories(Signature.class, signatureFactories,
                (parent == null) ? Collections.emptyList() : parent.getSignatureFactories());
    }

    @Override
    public void setSignatureFactories(List<NamedFactory<Signature>> signatureFactories) {
        this.signatureFactories = signatureFactories;
    }

    @Override
    public KeyPairProvider getKeyPairProvider() {
        return resolveEffectiveProvider(KeyPairProvider.class, keyPairProvider,
                (parent == null) ? null : parent.getKeyPairProvider());
    }

    @Override
    public void setKeyPairProvider(KeyPairProvider keyPairProvider) {
        this.keyPairProvider = keyPairProvider;
    }

    protected <V> List<NamedFactory<V>> resolveEffectiveFactories(Class<V> factoryType, List<NamedFactory<V>> local, List<NamedFactory<V>> inherited) {
        if (GenericUtils.isEmpty(local)) {
            return inherited;
        } else {
            return local;
        }
    }

    protected <V> V resolveEffectiveProvider(Class<V> providerType, V local, V inherited) {
        if (local == null) {
            return inherited;
        } else {
            return local;
        }
    }
}
