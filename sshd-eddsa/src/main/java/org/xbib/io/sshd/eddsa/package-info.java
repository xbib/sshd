/**
 * Java implementation of EdDSA, a digital signature scheme using
 * a variant of elliptic curve cryptography based on Twisted Edwards curves.
 * Contains a generic implementation for any curve using BigIntegers,
 * and an optimized implementation for Curve 25519 using Radix 2^51.
 */
package org.xbib.io.sshd.eddsa;
