package org.xbib.io.sshd.common.signature;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.NamedResource;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ValidateUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Manage the list of named factories for <code>Signature</code>.
 */
public interface SignatureFactoriesManager {
    /**
     * Attempts to use the primary manager's signature factories if not {@code null}/empty,
     * otherwise uses the secondary ones (regardless of whether there are any...)
     *
     * @param primary   The primary {@link SignatureFactoriesManager}
     * @param secondary The secondary {@link SignatureFactoriesManager}
     * @return The resolved signature factories - may be {@code null}/empty
     * @see #getSignatureFactories(SignatureFactoriesManager)
     */
    static List<NamedFactory<Signature>> resolveSignatureFactories(
            SignatureFactoriesManager primary, SignatureFactoriesManager secondary) {
        List<NamedFactory<Signature>> factories = getSignatureFactories(primary);
        return GenericUtils.isEmpty(factories) ? getSignatureFactories(secondary) : factories;
    }

    /**
     * @param manager The {@link SignatureFactoriesManager} instance - ignored if {@code null}
     * @return The associated list of named <code>Signature</code> factories or {@code null} if
     * no manager instance
     */
    static List<NamedFactory<Signature>> getSignatureFactories(SignatureFactoriesManager manager) {
        return (manager == null) ? null : manager.getSignatureFactories();
    }

    /**
     * @return The list of named <code>Signature</code> factories
     */
    List<NamedFactory<Signature>> getSignatureFactories();

    void setSignatureFactories(List<NamedFactory<Signature>> factories);

    default String getSignatureFactoriesNameList() {
        return NamedResource.getNames(getSignatureFactories());
    }

    default void setSignatureFactoriesNameList(String names) {
        setSignatureFactoriesNames(GenericUtils.split(names, ','));
    }

    default List<String> getSignatureFactoriesNames() {
        return NamedResource.getNameList(getSignatureFactories());
    }

    default void setSignatureFactoriesNames(Collection<String> names) {
        BuiltinSignatures.ParseResult result = BuiltinSignatures.parseSignatureList(names);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<NamedFactory<Signature>> factories =
                (List) ValidateUtils.checkNotNullAndNotEmpty(result.getParsedFactories(), "No supported signature factories: %s", names);
        Collection<String> unsupported = result.getUnsupportedFactories();
        ValidateUtils.checkTrue(GenericUtils.isEmpty(unsupported), "Unsupported signature factories found: %s", unsupported);
        setSignatureFactories(factories);
    }

    default void setSignatureFactoriesNames(String... names) {
        setSignatureFactoriesNames(GenericUtils.isEmpty((Object[]) names) ? Collections.emptyList() : Arrays.asList(names));
    }
}
