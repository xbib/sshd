package org.xbib.io.sshd.server.kex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to load DH group primes from a file.
 */
public final class Moduli {

    public static final int MODULI_TYPE_SAFE = 2;
    public static final int MODULI_TESTS_COMPOSITE = 0x01;

    // Private constructor
    private Moduli() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    public static List<DhGroup> parseModuli(URL url) throws IOException {
        List<DhGroup> groups = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                // Ensure valid line
                if (parts.length != 7) {
                    continue;
                }

                // Discard moduli types which are not safe
                int type = Integer.parseInt(parts[1]);
                if (type != MODULI_TYPE_SAFE) {
                    continue;
                }

                // Discard untested modulis
                int tests = Integer.parseInt(parts[2]);
                if ((tests & MODULI_TESTS_COMPOSITE) != 0 || (tests & ~MODULI_TESTS_COMPOSITE) == 0) {
                    continue;
                }

                // Discard untried
                int tries = Integer.parseInt(parts[3]);
                if (tries == 0) {
                    continue;
                }

                DhGroup group = new DhGroup();
                group.size = Integer.parseInt(parts[4]) + 1;
                group.g = new BigInteger(parts[5], 16);
                group.p = new BigInteger(parts[6], 16);
                groups.add(group);
            }

            return groups;
        }
    }

    public static class DhGroup {
        int size;
        BigInteger g;
        BigInteger p;
    }
}
