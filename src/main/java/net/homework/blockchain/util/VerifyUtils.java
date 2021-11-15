package net.homework.blockchain.util;

import net.homework.blockchain.bean.Transaction;

import java.util.List;

public class VerifyUtils {
    public static boolean isListEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isCoinbaseTx(Transaction tx) {
        if (tx.getInputs().size() == 1) {
            return isCoinbaseInput(tx.getInputs().get(0));
        } else {
            return false;
        }
    }

    public static boolean isCoinbaseInput(Transaction.Input input) {
        return ByteUtils.isZero(input.getPreviousTransactionHash()) && input.getOutIndex() == -1;
    }

    public static boolean isCoinbaseMature(Transaction refOutTx) {

    }

    public static boolean isOutputPresentInTx(Transaction toCheck, byte[] refOut, int outIndex) {
        return toCheck.hashTransaction() == refOut && toCheck.getOutputs().size() > outIndex;
    }

    public static boolean verifyInput(Transaction.Input input) {
       return CryptoUtils.verifyTransaction(input.getScriptSig(), input.getScriptPubKey()) == input.getPreviousTransactionHash();
    }

    public static boolean isOutputSpent() {
    }
}
