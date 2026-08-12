package pro.smdev.poly4j.model;

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
