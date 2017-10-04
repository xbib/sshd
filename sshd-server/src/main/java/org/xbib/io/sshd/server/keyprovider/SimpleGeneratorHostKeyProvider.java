package org.xbib.io.sshd.server.keyprovider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;

/**
 *
 */
public class SimpleGeneratorHostKeyProvider extends AbstractGeneratorHostKeyProvider {
    public SimpleGeneratorHostKeyProvider() {
        super();
    }

    public SimpleGeneratorHostKeyProvider(File file) {
        this((file == null) ? null : file.toPath());
    }

    public SimpleGeneratorHostKeyProvider(Path path) {
        setPath(path);
    }

    @Override
    protected KeyPair doReadKeyPair(String resourceKey, InputStream inputStream) throws IOException, GeneralSecurityException {
        try (ObjectInputStream r = new ObjectInputStream(inputStream)) {
            try {
                return (KeyPair) r.readObject();
            } catch (ClassNotFoundException e) {
                throw new InvalidKeySpecException("Missing classes: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void doWriteKeyPair(String resourceKey, KeyPair kp, OutputStream outputStream) throws IOException, GeneralSecurityException {
        try (ObjectOutputStream w = new ObjectOutputStream(outputStream)) {
            w.writeObject(kp);
        }
    }
}
