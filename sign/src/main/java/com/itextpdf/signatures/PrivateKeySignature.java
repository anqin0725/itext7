/*
 * $Id$
 *
 * This file is part of the iText (R) project.
    Copyright (c) 1998-2016 iText Group NV
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.signatures;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Implementation of the {@link IExternalSignature} interface that
 * can be used when you have a {@link PrivateKey} object.
 * @author Paulo Soares
 */
public class PrivateKeySignature implements IExternalSignature {

    /** The private key object. */
    private PrivateKey pk;

    /** The hash algorithm. */
    private String hashAlgorithm;

    /** The encryption algorithm (obtained from the private key) */
    private String encryptionAlgorithm;

    /** The security provider */
    private String provider;

    /**
     * Creates a {@link PrivateKeySignature} instance.
     * @param pk A {@link PrivateKey} object.
     * @param hashAlgorithm	A hash algorithm (e.g. "SHA-1", "SHA-256",...).
     * @param provider	A security provider (e.g. "BC").
     */
    public PrivateKeySignature(PrivateKey pk, String hashAlgorithm, String provider) {
        this.pk = pk;
        this.provider = provider;
        this.hashAlgorithm = DigestAlgorithms.getDigest(DigestAlgorithms.getAllowedDigest(hashAlgorithm));
        encryptionAlgorithm = pk.getAlgorithm();

        if (encryptionAlgorithm.startsWith("EC")) {
            encryptionAlgorithm = "ECDSA";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign(byte[] message) throws GeneralSecurityException {
        String signMode = hashAlgorithm + "with" + encryptionAlgorithm;
        Signature sig = provider == null ? Signature.getInstance(signMode) : Signature.getInstance(signMode, provider);
        sig.initSign(pk);
        sig.update(message);

        return sig.sign();
    }
}
