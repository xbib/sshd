package org.xbib.io.sshd.common.digest;

import org.xbib.io.sshd.common.NamedFactory;
import org.xbib.io.sshd.common.OptionalFeature;

/**
 *
 */
public interface DigestFactory extends DigestInformation, NamedFactory<Digest>, OptionalFeature {
    // nothing extra
}
