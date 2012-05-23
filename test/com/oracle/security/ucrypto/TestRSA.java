/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug     7088989
 * @summary Ensure the RSA ciphers and signatures works correctly
 */
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.math.*;
import javax.crypto.*;

public class TestRSA extends UcryptoTest {

    // KAT
    private static final byte PLAINTEXT[] = Arrays.copyOf
        (new String("Known plaintext message utilized" +
                    "for RSA Encryption &  Decryption" +
                    "block, SHA1, SHA256, SHA384  and" +
                    "SHA512 RSA Signature KAT tests.").getBytes(), 128);

    private static final byte MOD[] = {
        (byte)0xd5, (byte)0x84, (byte)0x95, (byte)0x07, (byte)0xf4, (byte)0xd0,
        (byte)0x1f, (byte)0x82, (byte)0xf3, (byte)0x79, (byte)0xf4, (byte)0x99,
        (byte)0x48, (byte)0x10, (byte)0xe1, (byte)0x71, (byte)0xa5, (byte)0x62,
        (byte)0x22, (byte)0xa3, (byte)0x4b, (byte)0x00, (byte)0xe3, (byte)0x5b,
        (byte)0x3a, (byte)0xcc, (byte)0x10, (byte)0x83, (byte)0xe0, (byte)0xaf,
        (byte)0x61, (byte)0x13, (byte)0x54, (byte)0x6a, (byte)0xa2, (byte)0x6a,
        (byte)0x2c, (byte)0x5e, (byte)0xb3, (byte)0xcc, (byte)0xa3, (byte)0x71,
        (byte)0x9a, (byte)0xb2, (byte)0x3e, (byte)0x78, (byte)0xec, (byte)0xb5,
        (byte)0x0e, (byte)0x6e, (byte)0x31, (byte)0x3b, (byte)0x77, (byte)0x1f,
        (byte)0x6e, (byte)0x94, (byte)0x41, (byte)0x60, (byte)0xd5, (byte)0x6e,
        (byte)0xd9, (byte)0xc6, (byte)0xf9, (byte)0x29, (byte)0xc3, (byte)0x40,
        (byte)0x36, (byte)0x25, (byte)0xdb, (byte)0xea, (byte)0x0b, (byte)0x07,
        (byte)0xae, (byte)0x76, (byte)0xfd, (byte)0x99, (byte)0x29, (byte)0xf4,
        (byte)0x22, (byte)0xc1, (byte)0x1a, (byte)0x8f, (byte)0x05, (byte)0xfe,
        (byte)0x98, (byte)0x09, (byte)0x07, (byte)0x05, (byte)0xc2, (byte)0x0f,
        (byte)0x0b, (byte)0x11, (byte)0x83, (byte)0x39, (byte)0xca, (byte)0xc7,
        (byte)0x43, (byte)0x63, (byte)0xff, (byte)0x33, (byte)0x80, (byte)0xe7,
        (byte)0xc3, (byte)0x78, (byte)0xae, (byte)0xf1, (byte)0x73, (byte)0x52,
        (byte)0x98, (byte)0x1d, (byte)0xde, (byte)0x5c, (byte)0x53, (byte)0x6e,
        (byte)0x01, (byte)0x73, (byte)0x0d, (byte)0x12, (byte)0x7e, (byte)0x77,
        (byte)0x03, (byte)0xf1, (byte)0xef, (byte)0x1b, (byte)0xc8, (byte)0xa8,
        (byte)0x0f, (byte)0x97
    };

    private static final byte PUB_EXP[] = {(byte)0x01, (byte)0x00, (byte)0x01};

    private static final byte PRIV_EXP[] = {
        (byte)0x85, (byte)0x27, (byte)0x47, (byte)0x61, (byte)0x4c, (byte)0xd4,
        (byte)0xb5, (byte)0xb2, (byte)0x0e, (byte)0x70, (byte)0x91, (byte)0x8f,
        (byte)0x3d, (byte)0x97, (byte)0xf9, (byte)0x5f, (byte)0xcc, (byte)0x09,
        (byte)0x65, (byte)0x1c, (byte)0x7c, (byte)0x5b, (byte)0xb3, (byte)0x6d,
        (byte)0x63, (byte)0x3f, (byte)0x7b, (byte)0x55, (byte)0x22, (byte)0xbb,
        (byte)0x7c, (byte)0x48, (byte)0x77, (byte)0xae, (byte)0x80, (byte)0x56,
        (byte)0xc2, (byte)0x10, (byte)0xd5, (byte)0x03, (byte)0xdb, (byte)0x31,
        (byte)0xaf, (byte)0x8d, (byte)0x54, (byte)0xd4, (byte)0x48, (byte)0x99,
        (byte)0xa8, (byte)0xc4, (byte)0x23, (byte)0x43, (byte)0xb8, (byte)0x48,
        (byte)0x0b, (byte)0xc7, (byte)0xbc, (byte)0xf5, (byte)0xcc, (byte)0x64,
        (byte)0x72, (byte)0xbf, (byte)0x59, (byte)0x06, (byte)0x04, (byte)0x1c,
        (byte)0x32, (byte)0xf5, (byte)0x14, (byte)0x2e, (byte)0x6e, (byte)0xe2,
        (byte)0x0f, (byte)0x5c, (byte)0xde, (byte)0x36, (byte)0x3c, (byte)0x6e,
        (byte)0x7c, (byte)0x4d, (byte)0xcc, (byte)0xd3, (byte)0x00, (byte)0x6e,
        (byte)0xe5, (byte)0x45, (byte)0x46, (byte)0xef, (byte)0x4d, (byte)0x25,
        (byte)0x46, (byte)0x6d, (byte)0x7f, (byte)0xed, (byte)0xbb, (byte)0x4f,
        (byte)0x4d, (byte)0x9f, (byte)0xda, (byte)0x87, (byte)0x47, (byte)0x8f,
        (byte)0x74, (byte)0x44, (byte)0xb7, (byte)0xbe, (byte)0x9d, (byte)0xf5,
        (byte)0xdd, (byte)0xd2, (byte)0x4c, (byte)0xa5, (byte)0xab, (byte)0x74,
        (byte)0xe5, (byte)0x29, (byte)0xa1, (byte)0xd2, (byte)0x45, (byte)0x3b,
        (byte)0x33, (byte)0xde, (byte)0xd5, (byte)0xae, (byte)0xf7, (byte)0x03,
        (byte)0x10, (byte)0x21
    };

    private static final byte PRIME_P[] = {
        (byte)0xf9, (byte)0x74, (byte)0x8f, (byte)0x16, (byte)0x02, (byte)0x6b,
        (byte)0xa0, (byte)0xee, (byte)0x7f, (byte)0x28, (byte)0x97, (byte)0x91,
        (byte)0xdc, (byte)0xec, (byte)0xc0, (byte)0x7c, (byte)0x49, (byte)0xc2,
        (byte)0x85, (byte)0x76, (byte)0xee, (byte)0x66, (byte)0x74, (byte)0x2d,
        (byte)0x1a, (byte)0xb8, (byte)0xf7, (byte)0x2f, (byte)0x11, (byte)0x5b,
        (byte)0x36, (byte)0xd8, (byte)0x46, (byte)0x33, (byte)0x3b, (byte)0xd8,
        (byte)0xf3, (byte)0x2d, (byte)0xa1, (byte)0x03, (byte)0x83, (byte)0x2b,
        (byte)0xec, (byte)0x35, (byte)0x43, (byte)0x32, (byte)0xff, (byte)0xdd,
        (byte)0x81, (byte)0x7c, (byte)0xfd, (byte)0x65, (byte)0x13, (byte)0x04,
        (byte)0x7c, (byte)0xfc, (byte)0x03, (byte)0x97, (byte)0xf0, (byte)0xd5,
        (byte)0x62, (byte)0xdc, (byte)0x0d, (byte)0xbf
    };

    private static final byte PRIME_Q[] = {
        (byte)0xdb, (byte)0x1e, (byte)0xa7, (byte)0x3d, (byte)0xe7, (byte)0xfa,
        (byte)0x8b, (byte)0x04, (byte)0x83, (byte)0x48, (byte)0xf3, (byte)0xa5,
        (byte)0x31, (byte)0x9d, (byte)0x35, (byte)0x5e, (byte)0x4d, (byte)0x54,
        (byte)0x77, (byte)0xcc, (byte)0x84, (byte)0x09, (byte)0xf3, (byte)0x11,
        (byte)0x0d, (byte)0x54, (byte)0xed, (byte)0x85, (byte)0x39, (byte)0xa9,
        (byte)0xca, (byte)0xa8, (byte)0xea, (byte)0xae, (byte)0x19, (byte)0x9c,
        (byte)0x75, (byte)0xdb, (byte)0x88, (byte)0xb8, (byte)0x04, (byte)0x8d,
        (byte)0x54, (byte)0xc6, (byte)0xa4, (byte)0x80, (byte)0xf8, (byte)0x93,
        (byte)0xf0, (byte)0xdb, (byte)0x19, (byte)0xef, (byte)0xd7, (byte)0x87,
        (byte)0x8a, (byte)0x8f, (byte)0x5a, (byte)0x09, (byte)0x2e, (byte)0x54,
        (byte)0xf3, (byte)0x45, (byte)0x24, (byte)0x29
    };

    private static final byte EXP_P[] = {
        (byte)0x6a, (byte)0xd1, (byte)0x25, (byte)0x80, (byte)0x18, (byte)0x33,
        (byte)0x3c, (byte)0x2b, (byte)0x44, (byte)0x19, (byte)0xfe, (byte)0xa5,
        (byte)0x40, (byte)0x03, (byte)0xc4, (byte)0xfc, (byte)0xb3, (byte)0x9c,
        (byte)0xef, (byte)0x07, (byte)0x99, (byte)0x58, (byte)0x17, (byte)0xc1,
        (byte)0x44, (byte)0xa3, (byte)0x15, (byte)0x7d, (byte)0x7b, (byte)0x22,
        (byte)0x22, (byte)0xdf, (byte)0x03, (byte)0x58, (byte)0x66, (byte)0xf5,
        (byte)0x24, (byte)0x54, (byte)0x52, (byte)0x91, (byte)0x2d, (byte)0x76,
        (byte)0xfe, (byte)0x63, (byte)0x64, (byte)0x4e, (byte)0x0f, (byte)0x50,
        (byte)0x2b, (byte)0x65, (byte)0x79, (byte)0x1f, (byte)0xf1, (byte)0xbf,
        (byte)0xc7, (byte)0x41, (byte)0x26, (byte)0xcc, (byte)0xc6, (byte)0x1c,
        (byte)0xa9, (byte)0x83, (byte)0x6f, (byte)0x03
    };

    private static final byte EXP_Q[] = {
        (byte)0x12, (byte)0x84, (byte)0x1a, (byte)0x99, (byte)0xce, (byte)0x9a,
        (byte)0x8b, (byte)0x58, (byte)0xcc, (byte)0x47, (byte)0x43, (byte)0xdf,
        (byte)0x77, (byte)0xbb, (byte)0xd3, (byte)0x20, (byte)0xae, (byte)0xe4,
        (byte)0x2e, (byte)0x63, (byte)0x67, (byte)0xdc, (byte)0xf7, (byte)0x5f,
        (byte)0x3f, (byte)0x83, (byte)0x27, (byte)0xb7, (byte)0x14, (byte)0x52,
        (byte)0x56, (byte)0xbf, (byte)0xc3, (byte)0x65, (byte)0x06, (byte)0xe1,
        (byte)0x03, (byte)0xcc, (byte)0x93, (byte)0x57, (byte)0x09, (byte)0x7b,
        (byte)0x6f, (byte)0xe8, (byte)0x81, (byte)0x4a, (byte)0x2c, (byte)0xb7,
        (byte)0x43, (byte)0xa9, (byte)0x20, (byte)0x1d, (byte)0xf6, (byte)0x56,
        (byte)0x8b, (byte)0xcc, (byte)0xe5, (byte)0x4c, (byte)0xd5, (byte)0x4f,
        (byte)0x74, (byte)0x67, (byte)0x29, (byte)0x51
    };

    private static final byte CRT_COEFF[] = {
        (byte)0x23, (byte)0xab, (byte)0xf4, (byte)0x03, (byte)0x2f, (byte)0x29,
        (byte)0x95, (byte)0x74, (byte)0xac, (byte)0x1a, (byte)0x33, (byte)0x96,
        (byte)0x62, (byte)0xed, (byte)0xf7, (byte)0xf6, (byte)0xae, (byte)0x07,
        (byte)0x2a, (byte)0x2e, (byte)0xe8, (byte)0xab, (byte)0xfb, (byte)0x1e,
        (byte)0xb9, (byte)0xb2, (byte)0x88, (byte)0x1e, (byte)0x85, (byte)0x05,
        (byte)0x42, (byte)0x64, (byte)0x03, (byte)0xb2, (byte)0x8b, (byte)0xc1,
        (byte)0x81, (byte)0x75, (byte)0xd7, (byte)0xba, (byte)0xaa, (byte)0xd4,
        (byte)0x31, (byte)0x3c, (byte)0x8a, (byte)0x96, (byte)0x23, (byte)0x9d,
        (byte)0x3f, (byte)0x06, (byte)0x3e, (byte)0x44, (byte)0xa9, (byte)0x62,
        (byte)0x2f, (byte)0x61, (byte)0x5a, (byte)0x51, (byte)0x82, (byte)0x2c,
        (byte)0x04, (byte)0x85, (byte)0x73, (byte)0xd1
    };

    private static KeyPair genRSAKey(int keyLength) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keyLength);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't generate key: " + e);
            return null;
        }
    }

    private static KeyPair genPredefinedRSAKeyPair() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            BigInteger mod = new BigInteger(MOD);
            BigInteger pub = new BigInteger(PUB_EXP);

            PrivateKey privKey = kf.generatePrivate
                (new RSAPrivateCrtKeySpec
                 (mod, pub, new BigInteger(PRIV_EXP),
                  new BigInteger(PRIME_P), new BigInteger(PRIME_Q),
                  new BigInteger(EXP_P), new BigInteger(EXP_Q),
                  new BigInteger(CRT_COEFF)));
            PublicKey pubKey = kf.generatePublic(new RSAPublicKeySpec(mod, pub));
            return new KeyPair(pubKey, privKey);
        } catch (NoSuchAlgorithmException|InvalidKeySpecException ex) {
            System.err.println("Couldn't generate predefined key pair: " + ex);
            return null;
        }
    }

    private static final String CIP_ALGOS[] = {
        "RSA/ECB/NoPadding",
        "RSA/ECB/PKCS1Padding"
    };
    private static final int INPUT_SIZE_REDUCTION[] = {
        0,
        11,
    };
    private static final String SIG_ALGOS[] = {
        "MD5WithRSA",
        "SHA1WithRSA",
        "SHA256WithRSA",
        "SHA384WithRSA",
        "SHA512WithRSA"
    };

    private static KeyPair kp[] = null;

    public static void main(String argv[]) throws Exception {
        main(new TestRSA(), null);
    }

    public boolean doTest(Provider prov) {
        // first test w/ predefine KeyPair
        KeyPair pkp = genPredefinedRSAKeyPair();
        if (pkp == null)
            return true;
        System.err.println("Test against Predefined RSA Key Pair");
        boolean result1 = testCipher(pkp, 128, true, prov);
        boolean result2 = testSignature(pkp, true, prov);

        boolean[] cResults = new boolean[10];
        boolean[] sResults = new boolean[10];
        for (int i = 0; i < 10; i++) {
            // then test w/ various key lengths
            int keyLens[] = { 1024, 2048 };
            kp = new KeyPair[keyLens.length];

            cResults[i] = testCipher(keyLens, false, prov);
            sResults[i] = testSignature(keyLens, false, prov);
        }
        boolean result3 = true;
        boolean result4 = true;
        for (int i = 0; i < 10; i++) {
            if (!cResults[i])
                result3 = false;
            if (!sResults[i])
                result4 = false;
        }
        return result1 && result2 && result3 && result4;
    }


    private static boolean testCipher(KeyPair kp, int inputSizeInBytes,
                                      boolean checkInterop, Provider prov) {
        Cipher c1, c2;
        boolean[] results = new boolean[CIP_ALGOS.length];
        for (int i = 0; i < CIP_ALGOS.length; i++) {
            String algo = CIP_ALGOS[i];
            try {
                c1 = Cipher.getInstance(algo, prov);
            } catch (NoSuchAlgorithmException|NoSuchPaddingException nsae) {
                System.err.println("Skip unsupported Cipher algo: " + algo);
                results[i] = true;
                continue;
            }

            try {
                if (checkInterop) {
                    c2 = Cipher.getInstance(algo, "SunJCE");
                } else {
                    c2 = Cipher.getInstance(algo, prov);
                }
            } catch (NoSuchAlgorithmException|NoSuchPaddingException|NoSuchProviderException nsae) {
                System.err.println("Skip unsupported Cipher algo: " + algo);
                results[i] = true;
                continue;
            }

            byte[] data = Arrays.copyOf
                 (PLAINTEXT, inputSizeInBytes - INPUT_SIZE_REDUCTION[i]);

            results[i] = testEncryption(c1, c2, kp, data);
        }
        for (int i = 0; i < CIP_ALGOS.length; i++)
            if (!results[i])
                return false;
        return true;
    }

    private static boolean testCipher(int keyLens[], boolean checkInterop,
                                      Provider prov) {
        // RSA CipherText will always differ due to the random nonce in padding
        // so we check whether both
        // 1) Java Encrypt/C Decrypt
        // 2) C Encrypt/Java Decrypt
        // works
        Cipher c1, c2;
        boolean[] results = new boolean[CIP_ALGOS.length];
        for (int i = 0; i < CIP_ALGOS.length; i++) {
            String algo = CIP_ALGOS[i];
            results[i] = true;
            try {
                c1 = Cipher.getInstance(algo, prov);
            } catch (NoSuchAlgorithmException|NoSuchPaddingException nsae) {
                System.err.println("Skip unsupported Cipher algo: " + algo);
                continue;
            }

            try {
                if (checkInterop) {
                    c2 = Cipher.getInstance(algo, "SunJCE");
                } else {
                    c2 = Cipher.getInstance(algo, prov);
                }
            } catch (NoSuchAlgorithmException|NoSuchPaddingException|NoSuchProviderException nsae) {
                System.err.println("Skip unsupported Cipher algo: " + algo);
                continue;
            }

            boolean[] encResults = new boolean[keyLens.length];
            for (int h = 0; h < keyLens.length; h++) {
                // Defer key pair generation until now when it'll soon be used.
                if (kp[h] == null) {
                    kp[h] = genRSAKey(keyLens[h]);
                    if (kp[h] == null) {
                        encResults[h] = true;
                        continue;
                    }
                }
                System.err.println("Testing Cipher " + algo + " w/ KeySize " + keyLens[h]);
                byte[] data = Arrays.copyOf
                    (PLAINTEXT, keyLens[h]/8 - INPUT_SIZE_REDUCTION[i]);
                encResults[h] = testEncryption(c1, c2, kp[h], data);
            }
            for (int h = 0; h < keyLens.length; h++)
                if (!encResults[h])
                    results[i] = false;
        }
        for (int i = 0; i < CIP_ALGOS.length; i++)
            if (!results[i])
                return false;
        return true;
    }

    private static boolean testEncryption(Cipher c1, Cipher c2, KeyPair kp, byte[] data) {
        // C1 Encrypt + C2 Decrypt
        byte[] out1 = null;
        byte[] recoveredText = null;
        boolean result1 = true;
        try {
            c1.init(Cipher.ENCRYPT_MODE, kp.getPublic());
            out1 = c1.doFinal(data);
            c2.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            recoveredText = c2.doFinal(out1);
        } catch (Exception ex) {
            System.err.println("DEC ERROR: unexpected exception: " + ex);
            ex.printStackTrace();
            result1 = false;
        }
        boolean result2 = true;
        if(!Arrays.equals(recoveredText, data)) {
            System.err.println("DEC ERROR: different PT bytes");
            result2 = false;
        }
        // C2 Encrypt + C1 Decrypt
        byte[] cipherText = null;
        boolean result3 = true;
        try {
            c2.init(Cipher.ENCRYPT_MODE, kp.getPublic());
            cipherText = c2.doFinal(data);
            c1.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            try {
                out1 = c1.doFinal(cipherText);
            } catch (Exception ex) {
                System.err.println("\tENC ERROR: invalid encrypted output: " + ex);
                ex.printStackTrace();
                result3 = false;
            }
        } catch (Exception ex) {
            System.err.println("ENC ERROR: unexpected exception: " + ex);
            ex.printStackTrace();
            result3 = false;
        }
        boolean result4 = Arrays.equals(out1, data);
        if (!result4) {
            System.err.println("ENC ERROR: Decrypted result DIFF");
        }
        if (result1 && result2 && result3 && result4) {
            System.err.println("=> PASS");
            return true;
        }
        return false;
    }

    private static boolean testSignature(KeyPair kp, boolean checkInterop,
                                         Provider prov) {
        byte[] data = PLAINTEXT;
        Signature sig1, sig2;
        boolean[] results = new boolean[SIG_ALGOS.length];
        for (int i = 0; i < SIG_ALGOS.length; i++) {
            String algo = SIG_ALGOS[i];
            try {
                sig1 = Signature.getInstance(algo, prov);
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("Skip unsupported Signature algo: " + algo);
                results[i] = true;
                continue;
            }

            try {
                if (checkInterop) {
                    sig2 = Signature.getInstance(algo, "SunRsaSign");
                } else {
                    sig2 = Signature.getInstance(algo, prov);
                }
            } catch (NoSuchAlgorithmException|NoSuchProviderException nsae) {
                System.err.println("Skip unsupported interop Signature algo: " + algo);
                results[i] = true;
                continue;
            }
            results[i] = testSigning(sig1, sig2, kp, data);
        }
        for (int i = 0; i < SIG_ALGOS.length; i++)
            if (!results[i])
                return false;
        return true;
    }

    private static boolean testSignature(int keyLens[], boolean checkInterop,
                                         Provider prov) {
        byte[] data = PLAINTEXT;
        Signature sig1, sig2;
        boolean[] results = new boolean[SIG_ALGOS.length];
        for (int i = 0; i < SIG_ALGOS.length; i++) {
            String algo = SIG_ALGOS[i];
            results[i] = true;
            try {
                sig1 = Signature.getInstance(algo, prov);
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("Skip unsupported Signature algo: " + algo);
                continue;
            }

            try {
                if (checkInterop) {
                    sig2 = Signature.getInstance(algo, "SunRsaSign");
                } else {
                    sig2 = Signature.getInstance(algo, prov);
                }
            } catch (NoSuchAlgorithmException|NoSuchProviderException nsae) {
                System.err.println("Skip unsupported Signature algo: " + algo);
                continue;
            }

            boolean[] subResults = new boolean[keyLens.length];
            for (int h = 0; h < keyLens.length; h++) {
                // Defer key pair generation until now when it'll soon be used.
                if (kp[h] == null) {
                    kp[h] = genRSAKey(keyLens[h]);
                    if (kp[h] == null) {
                        subResults[h] = true;
                        continue;
                    }
                }
                System.err.println("Testing Signature " + algo + " w/ KeySize " + keyLens[h]);

                subResults[h] = testSigning(sig1, sig2, kp[h], data);
            }
            for (int h = 0; h < keyLens.length; h++)
                if (!subResults[h])
                    results[i] = false;
        }
        for (int i = 0; i < SIG_ALGOS.length; i++)
            if (!results[i])
                return false;
        return true;
    }

    private static boolean testSigning(Signature sig1, Signature sig2, KeyPair kp, byte[] data) {
        boolean sameSig = false;
        byte[] out = null;
        boolean testInit = true;
        try {
            sig1.initSign(kp.getPrivate());
            sig1.update(data);
            out = sig1.sign();
        } catch (Exception ex) {
            System.err.println("SIGN ERROR: unexpected exception: " + ex);
            ex.printStackTrace();
            testInit = false;
        }

        boolean testInit2 = true;
        byte[] out2 = null;
        try {
            sig2.initSign(kp.getPrivate());
            sig2.update(data);
            out2 = sig2.sign();
        } catch (InvalidKeyException|SignatureException ex) {
            System.err.println("SIGN ERROR: unexpected exception " + ex);
            ex.printStackTrace();
            testInit2 = false;
        }
        boolean sigTestPassed = true;
        if (out2 == null || !Arrays.equals(out2, out)) {
            System.err.println("SIGN ERROR: Signature DIFF!");
            sigTestPassed = false;
        }

        boolean verify = false;
        try {
            System.err.println("VERIFY1 using native out");
            sig1.initVerify(kp.getPublic());
            sig1.update(data);
            verify = sig1.verify(out);
            if (!verify) {
                System.err.println("VERIFY1 FAIL!");
            }
        } catch (Exception ex) {
            System.err.println("VERIFY1 ERROR: unexpected exception: " + ex);
            ex.printStackTrace();
        }
        if (verify && sigTestPassed && testInit && testInit2) {
            System.err.println("=> PASS");
            return true;
        }
        return false;
    }
}
