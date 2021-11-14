package net.homework.blockchain;

import net.homework.blockchain.bean.Transaction;
import net.homework.blockchain.util.ByteUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws DecoderException {
        List<Transaction.Input> inputs = new ArrayList<>();
        inputs.add(new Transaction.Input(
                Hex.decodeHex("f5d8ee39a430901c91a5917b9f2dc19d6d1a0e9cea205b009ca73dd04470b9a6"),
                0,
                Hex.decodeHex("304502206e21798a42fae0e854281abd38bacd1aeed3ee3738d9e1446618c4571d10"),
                Hex.decodeHex("90db022100e2ac980643b0b82c0e88ffdfec6b64e3e6ba35e7ba5fdd7d5d6cc8d25c6b241501")));
        List<Transaction.Output> outputs = new ArrayList<>();
        outputs.add(new Transaction.Output(5000000000L, Hex.decodeHex("404371705fa9bd789a2fcd52d2c580b65d35549d")));
        Transaction tx = new Transaction(inputs, outputs);
        System.out.println(ByteUtils.toJson(tx));
    }
}
