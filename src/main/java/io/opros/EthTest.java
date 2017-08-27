package io.opros;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by anton on 27/08/2017.
 */
public class EthTest {
    public static void main(String[] args) throws Exception {
        Web3j web3 = Web3j.build(new HttpService());  // defaults to http://localhost:8545/
        Credentials credentials = WalletUtils.loadCredentials("123", "/var/folders/35/qblq10fs0xd0dglld1rz8my40000gn/T/ethereum_dev_mode/keystore/UTC--2017-08-27T00-29-54.608326416Z--c3878f6010777abe4296de6208c2ca46ed9ccd8e");
        TransactionReceipt transactionReceipt = Transfer.sendFunds(
                web3, credentials, "0x5e2e5b93bca911c2e3a275b7b43ebbbf4ca280ed", BigDecimal.valueOf(1.0), Convert.Unit.ETHER);

    }
}
