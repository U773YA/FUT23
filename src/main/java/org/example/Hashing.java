package org.example;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Hashing {
    public static void main(String[] args) {
        Set<Integer> numbers = Set.of(12345, 67890, 11111, 22222, 33333, 44444, 55555, 66666, 77777, 88888, 99999);
        UUID uuid = encode(numbers);
        System.out.println("UUID: " + uuid);
        Set<Integer> decodedNumbers = decode(uuid);
        System.out.println("Decoded numbers: " + Arrays.toString(decodedNumbers.toArray()));
    }

    private static final BigInteger MAX_NUMBER = BigInteger.valueOf(99999);
    private static final BigInteger MAX_PART = BigInteger.ONE.shiftLeft(16).subtract(BigInteger.ONE);
    private static final int NUM_PARTS = 11;

    public static UUID encode(Set<Integer> numbers) {
        BigInteger combined = BigInteger.ZERO;
        for (int num : numbers) {
            BigInteger bi = BigInteger.valueOf(num);
            combined = combined.shiftLeft(5).or(bi);
        }
        long mostSig = combined.shiftRight(64).longValue();
        long leastSig = combined.longValue();
        return new UUID(mostSig, leastSig);
    }

    public static Set<Integer> decode(UUID uuid) {
        long mostSig = uuid.getMostSignificantBits();
        long leastSig = uuid.getLeastSignificantBits();
        BigInteger combined = BigInteger.valueOf(mostSig).shiftLeft(64).or(BigInteger.valueOf(leastSig));

        Set<Integer> numbers = new HashSet<>();
        for (int i = 0; i < 11; i++) {
            BigInteger bi = combined.and(BigInteger.valueOf(0b11111));
            int num = bi.intValue();
            numbers.add(num);
            combined = combined.shiftRight(5);
        }
        return numbers;
    }

    private static BigInteger concatenate(BigInteger[] parts, int start, int end) {
        BigInteger result = BigInteger.ZERO;
        for (int i = end - 1; i >= start; i--) {
            result = result.or(parts[i].shiftLeft(16 * i));
        }
        return result;
    }
}
