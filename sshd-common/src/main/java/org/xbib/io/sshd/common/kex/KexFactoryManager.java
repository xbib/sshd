package org.xbib.io.sshd.common.kex;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.cipher.BuiltinCiphers;
import org.xbib.io.sshd.common.cipher.Cipher;
import org.xbib.io.sshd.common.compression.BuiltinCompressions;
import org.xbib.io.sshd.common.compression.Compression;
import org.xbib.io.sshd.common.keyprovider.KeyPairProviderHolder;
import org.xbib.io.sshd.common.mac.BuiltinMacs;
import org.xbib.io.sshd.common.mac.Mac;
import org.xbib.io.sshd.common.signature.SignatureFactoriesManager;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds KEX negotiation stage configuration.
 */
public interface KexFactoryManager extends KeyPairProviderHolder, SignatureFactoriesManager {
    /**
     * Retrieve the list of named factories for <code>KeyExchange</code>.
     *
     * @return a list of named <code>KeyExchange</code> factories, never {@code null}
     */
    List<NamedFactory<KeyExchange>> getKeyExchangeFactories();

    void setKeyExchangeFactories(List<NamedFactory<KeyExchange>> keyExchangeFactories);

    /**
     * Retrieve the list of named factories for <code>Cipher</code>.
     *
     * @return a list of named <code>Cipher</code> factories, never {@code null}
     */
    List<NamedFactory<Cipher>> getCipherFactories();

    void setCipherFactories(List<NamedFactory<Cipher>> cipherFactories);

    default String getCipherFactoriesNameList() {
        return NamedResource.getNames(getCipherFactories());
    }

    default void setCipherFactoriesNameList(String names) {
        setCipherFactoriesNames(GenericUtils.split(names, ','));
    }

    default List<String> getCipherFactoriesNames() {
        return NamedResource.getNameList(getCipherFactories());
    }

    default void setCipherFactoriesNames(Collection<String> names) {
        BuiltinCiphers.ParseResult result = BuiltinCiphers.parseCiphersList(names);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<NamedFactory<Cipher>> factories =
                (List) ValidateUtils.checkNotNullAndNotEmpty(result.getParsedFactories(), "No supported cipher factories: %s", names);
        Collection<String> unsupported = result.getUnsupportedFactories();
        ValidateUtils.checkTrue(GenericUtils.isEmpty(unsupported), "Unsupported cipher factories found: %s", unsupported);
        setCipherFactories(factories);
    }

    default void setCipherFactoriesNames(String... names) {
        setCipherFactoriesNames(GenericUtils.isEmpty((Object[]) names) ? Collections.emptyList() : Arrays.asList(names));
    }

    /**
     * Retrieve the list of named factories for <code>Compression</code>.
     *
     * @return a list of named <code>Compression</code> factories, never {@code null}
     */
    List<NamedFactory<Compression>> getCompressionFactories();

    void setCompressionFactories(List<NamedFactory<Compression>> compressionFactories);

    default String getCompressionFactoriesNameList() {
        return NamedResource.getNames(getCompressionFactories());
    }

    default void setCompressionFactoriesNameList(String names) {
        setCompressionFactoriesNames(GenericUtils.split(names, ','));
    }

    default List<String> getCompressionFactoriesNames() {
        return NamedResource.getNameList(getCompressionFactories());
    }

    default void setCompressionFactoriesNames(Collection<String> names) {
        BuiltinCompressions.ParseResult result = BuiltinCompressions.parseCompressionsList(names);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<NamedFactory<Compression>> factories =
                (List) ValidateUtils.checkNotNullAndNotEmpty(result.getParsedFactories(), "No supported compression factories: %s", names);
        Collection<String> unsupported = result.getUnsupportedFactories();
        ValidateUtils.checkTrue(GenericUtils.isEmpty(unsupported), "Unsupported compression factories found: %s", unsupported);
        setCompressionFactories(factories);
    }

    default void setCompressionFactoriesNames(String... names) {
        setCompressionFactoriesNames(GenericUtils.isEmpty((Object[]) names) ? Collections.emptyList() : Arrays.asList(names));
    }

    /**
     * Retrieve the list of named factories for <code>Mac</code>.
     *
     * @return a list of named <code>Mac</code> factories, never {@code null}
     */
    List<NamedFactory<Mac>> getMacFactories();

    void setMacFactories(List<NamedFactory<Mac>> macFactories);

    default String getMacFactoriesNameList() {
        return NamedResource.getNames(getMacFactories());
    }

    default void setMacFactoriesNameList(String names) {
        setMacFactoriesNames(GenericUtils.split(names, ','));
    }

    default List<String> getMacFactoriesNames() {
        return NamedResource.getNameList(getMacFactories());
    }

    default void setMacFactoriesNames(Collection<String> names) {
        BuiltinMacs.ParseResult result = BuiltinMacs.parseMacsList(names);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<NamedFactory<Mac>> factories =
                (List) ValidateUtils.checkNotNullAndNotEmpty(result.getParsedFactories(), "No supported MAC factories: %s", names);
        Collection<String> unsupported = result.getUnsupportedFactories();
        ValidateUtils.checkTrue(GenericUtils.isEmpty(unsupported), "Unsupported MAC factories found: %s", unsupported);
        setMacFactories(factories);
    }

    default void setMacFactoriesNames(String... names) {
        setMacFactoriesNames(GenericUtils.isEmpty((Object[]) names) ? Collections.emptyList() : Arrays.asList(names));
    }
}
