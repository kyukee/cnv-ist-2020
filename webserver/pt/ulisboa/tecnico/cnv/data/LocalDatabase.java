package pt.ulisboa.tecnico.cnv.data;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// central location in the server to save some temporary data before saving it to DynamoDB

public class LocalDatabase {

    // basic blocks
    private static Map<Long, BigInteger> b_count = new ConcurrentHashMap<Long, BigInteger>();

    public static BigInteger getBBCount(Long threadID) {
        return b_count.get(threadID);
    }

    public static void setBBCount(Long threadID, BigInteger count) {
        b_count.put(threadID, count);
    }

}
