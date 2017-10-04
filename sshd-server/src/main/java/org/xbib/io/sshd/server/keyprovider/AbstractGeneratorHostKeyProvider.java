package org.xbib.io.sshd.server.keyprovider;

import org.xbib.io.sshd.common.cipher.ECCurves;
import org.xbib.io.sshd.common.config.keys.BuiltinIdentities;
import org.xbib.io.sshd.common.config.keys.KeyUtils;
import org.xbib.io.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.xbib.io.sshd.common.util.io.IoUtils;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds a <U>single</U> {@link KeyPair} which is generated the 1st time
 * {@link #loadKeys()} is called. If there is a file backing it up and the
 * file exists, the key is loaded from it. Otherwise a new key pair is
 * generated and saved (provided a path is configured and {@link #isOverwriteAllowed()}
 */
public abstract class AbstractGeneratorHostKeyProvider extends AbstractKeyPairProvider {
    public static final String DEFAULT_ALGORITHM = KeyUtils.RSA_ALGORITHM;
    public static final boolean DEFAULT_ALLOWED_TO_OVERWRITE = true;

    private final AtomicReference<KeyPair> keyPairHolder = new AtomicReference<>();

    private Path path;
    private String algorithm = DEFAULT_ALGORITHM;
    private int keySize;
    private AlgorithmParameterSpec keySpec;
    private boolean overwriteAllowed = DEFAULT_ALLOWED_TO_OVERWRITE;

    protected AbstractGeneratorHostKeyProvider() {
        super();
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = (path == null) ? null : path.toAbsolutePath();
    }

    public void setFile(File file) {
        setPath((file == null) ? null : file.toPath());
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public AlgorithmParameterSpec getKeySpec() {
        return keySpec;
    }

    public void setKeySpec(AlgorithmParameterSpec keySpec) {
        this.keySpec = keySpec;
    }

    public boolean isOverwriteAllowed() {
        return overwriteAllowed;
    }

    public void setOverwriteAllowed(boolean overwriteAllowed) {
        this.overwriteAllowed = overwriteAllowed;
    }

    public void clearLoadedKeys() {
        KeyPair kp;
        synchronized (keyPairHolder) {
            kp = keyPairHolder.getAndSet(null);
        }
    }

    @Override   // co-variant return
    public synchronized List<KeyPair> loadKeys() {
        Path keyPath = getPath();
        KeyPair kp;
        synchronized (keyPairHolder) {
            kp = keyPairHolder.get();
            if (kp == null) {
                try {
                    kp = resolveKeyPair(keyPath);
                    if (kp != null) {
                        keyPairHolder.set(kp);
                    }
                } catch (Throwable t) {
                }
            }
        }

        if (kp == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(kp);
        }
    }

    protected KeyPair resolveKeyPair(Path keyPath) throws IOException, GeneralSecurityException {
        String alg = getAlgorithm();
        KeyPair kp;
        if (keyPath != null) {
            try {
                kp = loadFromFile(alg, keyPath);
                if (kp != null) {
                    return kp;
                }
            } catch (Throwable e) {
            }
        }

        // either no file specified or no key in file
        try {
            kp = generateKeyPair(alg);
            if (kp == null) {
                return null;
            }

        } catch (Throwable e) {
            return null;
        }

        if (keyPath != null) {
            try {
                writeKeyPair(kp, keyPath);
            } catch (Throwable e) {
            }
        }

        return kp;
    }

    protected KeyPair loadFromFile(String alg, Path keyPath) throws IOException, GeneralSecurityException {
        LinkOption[] options = IoUtils.getLinkOptions(true);
        if ((!Files.exists(keyPath, options)) || (!Files.isRegularFile(keyPath, options))) {
            return null;
        }

        KeyPair kp = readKeyPair(keyPath, IoUtils.EMPTY_OPEN_OPTIONS);
        if (kp == null) {
            return null;
        }

        PublicKey key = kp.getPublic();
        String keyAlgorithm = key.getAlgorithm();
        if (BuiltinIdentities.Constants.ECDSA.equalsIgnoreCase(keyAlgorithm)) {
            keyAlgorithm = KeyUtils.EC_ALGORITHM;
        } else if (BuiltinIdentities.Constants.ED25519.equalsIgnoreCase(keyAlgorithm)) {
            keyAlgorithm = SecurityUtils.EDDSA;
        }

        if (Objects.equals(alg, keyAlgorithm)) {
            return kp;
        }

        Files.deleteIfExists(keyPath);
        return null;
    }

    protected KeyPair readKeyPair(Path keyPath, OpenOption... options) throws IOException, GeneralSecurityException {
        try (InputStream inputStream = Files.newInputStream(keyPath, options)) {
            return doReadKeyPair(keyPath.toString(), inputStream);
        }
    }

    protected KeyPair doReadKeyPair(String resourceKey, InputStream inputStream) throws IOException, GeneralSecurityException {
        return SecurityUtils.loadKeyPairIdentity(resourceKey, inputStream, null);
    }

    protected void writeKeyPair(KeyPair kp, Path keyPath, OpenOption... options) throws IOException, GeneralSecurityException {
        if ((!Files.exists(keyPath)) || isOverwriteAllowed()) {
            try (OutputStream os = Files.newOutputStream(keyPath, options)) {
                doWriteKeyPair(keyPath.toString(), kp, os);
            } catch (Throwable e) {
            }
        } else {
        }
    }

    protected abstract void doWriteKeyPair(String resourceKey, KeyPair kp, OutputStream outputStream) throws IOException, GeneralSecurityException;

    protected KeyPair generateKeyPair(String algorithm) throws GeneralSecurityException {
        KeyPairGenerator generator = SecurityUtils.getKeyPairGenerator(algorithm);
        if (keySpec != null) {
            generator.initialize(keySpec);
            log.info("generateKeyPair(" + algorithm + ") generating host key - spec=" + keySpec.getClass().getSimpleName());
        } else if (keySize != 0) {
            generator.initialize(keySize);
            log.info("generateKeyPair(" + algorithm + ") generating host key - size=" + keySize);
        } else if (KeyUtils.EC_ALGORITHM.equals(algorithm)) {
            // If left to our own devices choose the biggest key size possible
            int numCurves = ECCurves.SORTED_KEY_SIZE.size();
            ECCurves curve = ECCurves.SORTED_KEY_SIZE.get(numCurves - 1);
            generator.initialize(curve.getParameters());
        }

        return generator.generateKeyPair();
    }
}
