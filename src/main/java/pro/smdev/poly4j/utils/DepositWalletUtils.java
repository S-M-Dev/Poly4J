package pro.smdev.poly4j.utils;

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

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.web3j.crypto.Keys.toChecksumAddress;

/**
 * Derives a Polymarket "deposit wallet" address (a beacon-proxy smart contract wallet) for a given
 * EOA signer address. This is a pure, offline CREATE2 derivation against the production
 * deposit-wallet factory/beacon contracts &mdash; no network call is required, and it does not
 * depend on the relayer's {@code proxyAddress} response field, which may be returned empty.
 *
 * <p>Mirrors {@code derive_beacon_deposit_wallet_address} in Polymarket's official {@code py-sdk}
 * (chain id 137 / Polygon mainnet), and is cross-checked against its golden test vector: signer
 * {@code 0x0000000000000000000000000000000000000001} derives to
 * {@code 0x94Bf330955A0B957662FEaF878DE77bF25f76CD9}.
 *
 * @author ALazyGuy
 * @since 1.0.0
 */
public class DepositWalletUtils {

    private static final String DEPOSIT_WALLET_FACTORY = "0x00000000000Fb5C9ADea0298D729A0CB3823Cc07";
    private static final String DEPOSIT_WALLET_BEACON = "0x7A18EDfe055488A3128f01F563e5B479D92ffc3a";

    private static final BigInteger ERC1967_BEACON_PREFIX_BASE = new BigInteger("6100523D8160233D3973", 16);
    private static final byte[] ERC1967_BEACON_CONST1 =
            Numeric.hexStringToByteArray("b3582b35133d50545afa5036515af43d6000803e604d573d6000fd5b3d6000f3");
    private static final byte[] ERC1967_BEACON_CONST2 =
            Numeric.hexStringToByteArray("1b60e01b36527fa3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6c");
    private static final byte[] ERC1967_BEACON_CONST3 =
            Numeric.hexStringToByteArray("60195155f3363d3d373d3d363d602036600436635c60da");

    public static String deriveDepositWalletAddress(String signerAddress) {
        byte[] args = depositWalletArgs(signerAddress);
        byte[] bytecodeHash = beaconDepositInitCodeHash(args);
        byte[] salt = Hash.sha3(args);
        return create2Address(DEPOSIT_WALLET_FACTORY, salt, bytecodeHash);
    }

    private static byte[] depositWalletArgs(String signerAddress) {
        return concat(addressWord(DEPOSIT_WALLET_FACTORY), addressWord(signerAddress));
    }

    private static byte[] beaconDepositInitCodeHash(byte[] args) {
        BigInteger prefix = ERC1967_BEACON_PREFIX_BASE.add(BigInteger.valueOf(args.length).shiftLeft(56));
        byte[] prefixBytes = toBigEndianBytes(prefix, 10);
        byte[] beaconBytes = Numeric.hexStringToByteArray(DEPOSIT_WALLET_BEACON);
        byte[] bytecode = concat(prefixBytes, beaconBytes, ERC1967_BEACON_CONST3, ERC1967_BEACON_CONST2,
                ERC1967_BEACON_CONST1, args);
        return Hash.sha3(bytecode);
    }

    private static String create2Address(String factory, byte[] salt, byte[] bytecodeHash) {
        byte[] factoryBytes = Numeric.hexStringToByteArray(factory);
        byte[] raw = concat(new byte[]{(byte) 0xff}, factoryBytes, salt, bytecodeHash);
        byte[] hash = Hash.sha3(raw);
        byte[] addressBytes = new byte[20];
        System.arraycopy(hash, 12, addressBytes, 0, 20);
        return toChecksumAddress("0x" + Numeric.toHexStringNoPrefix(addressBytes));
    }

    static byte[] addressWord(String address) {
        byte[] addressBytes = Numeric.hexStringToByteArray(address);
        byte[] word = new byte[32];
        System.arraycopy(addressBytes, 0, word, 32 - addressBytes.length, addressBytes.length);
        return word;
    }

    private static byte[] toBigEndianBytes(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        byte[] result = new byte[length];
        int copyLength = Math.min(raw.length, length);
        System.arraycopy(raw, raw.length - copyLength, result, length - copyLength, copyLength);
        return result;
    }

    static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) {
            total += a.length;
        }
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}
