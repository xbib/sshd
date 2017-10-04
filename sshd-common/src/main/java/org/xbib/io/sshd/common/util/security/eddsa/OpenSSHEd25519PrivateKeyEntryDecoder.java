package org.xbib.io.sshd.common.util.security.eddsa;

import org.xbib.io.sshd.eddsa.EdDSAPrivateKey;
import org.xbib.io.sshd.eddsa.EdDSAPublicKey;
import org.xbib.io.sshd.eddsa.spec.EdDSANamedCurveTable;
import org.xbib.io.sshd.eddsa.spec.EdDSAParameterSpec;
import org.xbib.io.sshd.eddsa.spec.EdDSAPrivateKeySpec;
import org.xbib.io.sshd.eddsa.spec.EdDSAPublicKeySpec;
import org.xbib.io.sshd.common.config.keys.FilePasswordProvider;
import org.xbib.io.sshd.common.config.keys.KeyEntryResolver;
import org.xbib.io.sshd.common.config.keys.impl.AbstractPrivateKeyEntryDecoder;
import org.xbib.io.sshd.common.keyprovider.KeyPairProvider;
import org.xbib.io.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

/**
 *
 */
public class OpenSSHEd25519PrivateKeyEntryDecoder extends AbstractPrivateKeyEntryDecoder<EdDSAPublicKey, EdDSAPrivateKey> {
    public static final OpenSSHEd25519PrivateKeyEntryDecoder INSTANCE = new OpenSSHEd25519PrivateKeyEntryDecoder();
    private static final int PK_SIZE = 32;
    private static final int SK_SIZE = 32;
    private static final int KEYPAIR_SIZE = PK_SIZE + SK_SIZE;

    public OpenSSHEd25519PrivateKeyEntryDecoder() {
        super(EdDSAPublicKey.class, EdDSAPrivateKey.class, Collections.unmodifiableList(Collections.singletonList(KeyPairProvider.SSH_ED25519)));
    }

    @Override
    public EdDSAPrivateKey decodePrivateKey(String keyType, FilePasswordProvider passwordProvider, InputStream keyData)
            throws IOException, GeneralSecurityException {
        if (!KeyPairProvider.SSH_ED25519.equals(keyType)) {
            throw new InvalidKeyException("Unsupported key type: " + keyType);
        }

        if (!SecurityUtils.isEDDSACurveSupported()) {
            throw new NoSuchAlgorithmException(SecurityUtils.EDDSA + " provider not supported");
        }

        // ed25519 bernstein naming: pk .. public key, sk .. secret key
        // we expect to find two byte arrays with the following structure (type:size):
        // [pk:32], [sk:32,pk:32]

        byte[] pk = KeyEntryResolver.readRLEBytes(keyData);
        byte[] keypair = KeyEntryResolver.readRLEBytes(keyData);

        if (pk.length != PK_SIZE) {
            throw new InvalidKeyException(String.format(Locale.ENGLISH, "Unexpected pk size: %s (expected %s)", pk.length, PK_SIZE));
        }

        if (keypair.length != KEYPAIR_SIZE) {
            throw new InvalidKeyException(String.format(Locale.ENGLISH, "Unexpected keypair size: %s (expected %s)", keypair.length, KEYPAIR_SIZE));
        }

        byte[] sk = Arrays.copyOf(keypair, SK_SIZE);

        // verify that the keypair contains the expected pk
        // yes, it's stored redundant, this seems to mimic the output structure of the keypair generation interface
        if (!Arrays.equals(pk, Arrays.copyOfRange(keypair, SK_SIZE, KEYPAIR_SIZE))) {
            throw new InvalidKeyException("Keypair did not contain the public key.");
        }

        // create the private key
        EdDSAParameterSpec params = EdDSANamedCurveTable.getByName(EdDSASecurityProviderUtils.CURVE_ED25519_SHA512);
        EdDSAPrivateKey privateKey = generatePrivateKey(new EdDSAPrivateKeySpec(sk, params));

        // the private key class contains the calculated public key (Abyte)
        // pointers to the corresponding code:
        // EdDSAPrivateKeySpec.EdDSAPrivateKeySpec(byte[], EdDSAParameterSpec): A = spec.getB().scalarMultiply(a);
        // EdDSAPrivateKey.EdDSAPrivateKey(EdDSAPrivateKeySpec): this.Abyte = this.A.toByteArray();

        // we can now verify the generated pk matches the one we read
        if (!Arrays.equals(privateKey.getAbyte(), pk)) {
            throw new InvalidKeyException("The provided pk does NOT match the computed pk for the given sk.");
        }

        return privateKey;
    }

    @Override
    public String encodePrivateKey(OutputStream s, EdDSAPrivateKey key) throws IOException {
        Objects.requireNonNull(key, "No private key provided");

        // ed25519 bernstein naming: pk .. public key, sk .. secret key
        // we are expected to write the following arrays (type:size):
        // [pk:32], [sk:32,pk:32]

        byte[] sk = key.getSeed();
        byte[] pk = key.getAbyte();

        Objects.requireNonNull(sk, "No seed");

        byte[] keypair = new byte[KEYPAIR_SIZE];
        System.arraycopy(sk, 0, keypair, 0, SK_SIZE);
        System.arraycopy(pk, 0, keypair, SK_SIZE, PK_SIZE);

        KeyEntryResolver.writeRLEBytes(s, pk);
        KeyEntryResolver.writeRLEBytes(s, keypair);

        return KeyPairProvider.SSH_ED25519;
    }

    @Override
    public boolean isPublicKeyRecoverySupported() {
        return true;
    }

    @Override
    public EdDSAPublicKey recoverPublicKey(EdDSAPrivateKey prvKey) throws GeneralSecurityException {
        return EdDSASecurityProviderUtils.recoverEDDSAPublicKey(prvKey);
    }

    @Override
    public EdDSAPublicKey clonePublicKey(EdDSAPublicKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        } else {
            return generatePublicKey(new EdDSAPublicKeySpec(key.getA(), key.getParams()));
        }
    }

    @Override
    public EdDSAPrivateKey clonePrivateKey(EdDSAPrivateKey key) throws GeneralSecurityException {
        if (key == null) {
            return null;
        } else {
            return generatePrivateKey(new EdDSAPrivateKeySpec(key.getSeed(), key.getParams()));
        }
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator() throws GeneralSecurityException {
        return SecurityUtils.getKeyPairGenerator(SecurityUtils.EDDSA);
    }

    @Override
    public KeyFactory getKeyFactoryInstance() throws GeneralSecurityException {
        return SecurityUtils.getKeyFactory(SecurityUtils.EDDSA);
    }
}