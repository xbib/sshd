package org.xbib.io.sshd.common.config.keys.loader.pem;

import org.xbib.io.sshd.common.config.keys.loader.KeyPairResourceParser;

/**
 *
 */
public interface KeyPairPEMResourceParser extends KeyPairResourceParser {
    /**
     * @return The encryption algorithm name - e.g., &quot;RSA&quot;, &quot;DSA&quot;
     */
    String getAlgorithm();

    /**
     * @return The OID used to identify this algorithm in DER encodings - e.g., RSA=1.2.840.113549.1.1.1
     */
    String getAlgorithmIdentifier();
}
