package pro.smdev.poly4j.model;

/*
 * Copyright 2026 S-M-Dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import pro.smdev.poly4j.utils.DepositWalletUtils;

/**
 * Holds a Polymarket account's authentication state: the signer's address and private key, the
 * derived deposit-wallet ({@code fund}) address, and the {@link Secrets} for the CLOB and builder APIs
 * once they have been derived or set.
 *
 * @author ALazyGuy
 * @since 2.0.0
 */
public class Authentication {
    private final String fundAddress;
    private final String signerAddress;
    private final String signerPrivateKey;
    private volatile Secrets builderSecrets = null;
    private volatile Secrets clobSecrets = null;

    /**
     * Creates an authentication with builder API credentials already set, in addition to the signer wallet
     * derived from {@code signerPrivateKey}.
     *
     * @param signerPrivateKey hex-encoded EOA private key (with or without {@code 0x} prefix)
     * @param builderApiKey    builder API key
     * @param builderSecret    builder API secret used to sign L2-style headers
     * @param builderPassphrase builder API passphrase
     */
    public Authentication(String signerPrivateKey, String builderApiKey, String builderSecret, String builderPassphrase) {
        this(signerPrivateKey);
        setBuilderSecrets(new Secrets(builderApiKey, builderSecret, builderPassphrase));
    }

    /**
     * Creates an authentication for the wallet derived from {@code signerPrivateKey}, deriving both its
     * checksummed address and its deposit-wallet ({@code fund}) address.
     *
     * @param signerPrivateKey hex-encoded EOA private key (with or without {@code 0x} prefix)
     */
    public Authentication(String signerPrivateKey) {
        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(signerPrivateKey));
        this.signerAddress = Keys.toChecksumAddress(Keys.getAddress(keyPair));
        this.signerPrivateKey = signerPrivateKey;
        this.fundAddress = DepositWalletUtils.deriveDepositWalletAddress(signerAddress);
    }

    /**
     * Sets the builder API {@link Secrets}, e.g. after {@link pro.smdev.poly4j.factory.BuilderRequestFactory}
     * calls or when they are already known.
     *
     * @param builderSecrets builder API credentials
     */
    public void setBuilderSecrets(Secrets builderSecrets) {
        this.builderSecrets = builderSecrets;
    }

    /**
     * Sets the CLOB API {@link Secrets}, normally called by {@link pro.smdev.poly4j.core.PolyClient} once
     * L2 credentials have been derived or created.
     *
     * @param clobSecrets CLOB API credentials
     */
    public void setClobSecrets(Secrets clobSecrets) {
        this.clobSecrets = clobSecrets;
    }

    /**
     * @return the builder API {@link Secrets}, or {@code null} if not yet set
     */
    public Secrets getBuilderSecrets() {
        return builderSecrets;
    }

    /**
     * @return the derived deposit-wallet ({@code fund}) address
     */
    public String getFundAddress() {
        return fundAddress;
    }

    /**
     * @return the checksummed EOA signer address
     */
    public String getSignerAddress() {
        return signerAddress;
    }

    /**
     * @return the EOA signer's private key, as supplied to the constructor
     */
    public String getSignerPrivateKey() {
        return signerPrivateKey;
    }

    /**
     * @return the CLOB API {@link Secrets}, or {@code null} if not yet derived
     */
    public Secrets getClobSecrets() {
        return clobSecrets;
    }
}
