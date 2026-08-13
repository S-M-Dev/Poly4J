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

public class Authentication {
    private final String fundAddress;
    private final String signerAddress;
    private final String signerPrivateKey;
    private Secrets builderSecrets = null;
    private Secrets clobSecrets = null;

    public Authentication(String signerPrivateKey) {
        ECKeyPair keyPair = ECKeyPair.create(Numeric.hexStringToByteArray(signerPrivateKey));
        this.signerAddress = Keys.toChecksumAddress(Keys.getAddress(keyPair));
        this.signerPrivateKey = signerPrivateKey;
        this.fundAddress = DepositWalletUtils.deriveDepositWalletAddress(signerAddress);
    }

    public void setBuilderSecrets(Secrets builderSecrets) {
        this.builderSecrets = builderSecrets;
    }

    public void setClobSecrets(Secrets clobSecrets) {
        this.clobSecrets = clobSecrets;
    }

    public Secrets getBuilderSecrets() {
        return builderSecrets;
    }

    public String getFundAddress() {
        return fundAddress;
    }

    public String getSignerAddress() {
        return signerAddress;
    }

    public String getSignerPrivateKey() {
        return signerPrivateKey;
    }

    public Secrets getClobSecrets() {
        return clobSecrets;
    }
}
