package org.xbib.io.sshd.server.util.security.bouncycastle;

import org.xbib.io.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

/**
 *
 */
public class BouncyCastleGeneratorHostKeyProvider extends AbstractGeneratorHostKeyProvider {
    public BouncyCastleGeneratorHostKeyProvider(Path path) {
        setPath(path);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void doWriteKeyPair(String resourceKey, KeyPair kp, OutputStream outputStream) throws IOException, GeneralSecurityException {
        try (org.bouncycastle.openssl.PEMWriter w =
                     new org.bouncycastle.openssl.PEMWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            w.writeObject(kp);
            w.flush();
        }
    }
}