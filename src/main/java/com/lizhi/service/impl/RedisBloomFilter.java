package com.lizhi.service.impl;

import com.lizhi.service.IRedisBloomFilter;
import com.lizhi.service.IRedisService;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * from https://blog.csdn.net/qq_18495465/article/details/78500472
 *
 * @param <E>
 */
public class RedisBloomFilter<E> implements IRedisBloomFilter<E> {

    private IRedisService redisService;

    private long expireDays = 50 * 6000;

    // total length of the Bloom filter
    private int sizeOfBloomFilter;
    // expected (maximum) number of elements to be added
    private int expectedNumberOfFilterElements;
    // number of hash functions
    private int numberOfHashFunctions;
    // encoding used for storing hash values as strings
    private final Charset charset = Charset.forName("UTF-8");
    // MD5 gives good enough accuracy in most circumstances. Change to SHA1 if it's needed
    private static final String hashName = "MD5";
    private static final MessageDigest digestFunction;

    // The digest method is reused between instances
    static {
        MessageDigest tmp;
        try {
            tmp = java.security.MessageDigest.getInstance(hashName);
        } catch (NoSuchAlgorithmException e) {
            tmp = null;
        }
        digestFunction = tmp;
    }

    public RedisBloomFilter() {
        this(0.0001, 600000);
    }

    /**
     * Constructs an empty Bloom filter.
     *
     * @param m is the total length of the Bloom filter.
     * @param n is the expected number of elements the filter will contain.
     * @param k is the number of hash functions used.
     */
    public RedisBloomFilter(int m, int n, int k) {
        this.sizeOfBloomFilter = m;
        this.expectedNumberOfFilterElements = n;
        this.numberOfHashFunctions = k;
    }

    /**
     * Constructs an empty Bloom filter with a given false positive probability.
     * The size of bloom filter and the number of hash functions is estimated
     * to match the false positive probability.
     *
     * @param falsePositiveProbability is the desired false positive probability.
     * @param expectedNumberOfElements is the expected number of elements in the Bloom filter.
     */
    public RedisBloomFilter(double falsePositiveProbability, int expectedNumberOfElements) {
        this((int) Math.ceil((int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) * expectedNumberOfElements / Math.log(2)), // m = ceil(kn/ln2)
                expectedNumberOfElements,
                (int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2)))); // k = ceil(-ln(f)/ln2)
    }

    public void init(String key, long expireTime, TimeUnit timeUnit) {
        redisService.setBit(key,0,false,expireTime,timeUnit);
    }

    public void init(String key) {
        redisService.setBit(key,0,false,expireDays, TimeUnit.DAYS);
    }

    public void setRedisService(IRedisService redisService) {
       this.redisService = redisService;
    }

    /**
     * Adds an object to the Bloom filter. The output from the object's
     * toString() method is used as input to the hash functions.
     *
     * @param element is an element to register in the Bloom filter.
     */
    public void add(String key, E element) {
        add(key, element.toString().getBytes(charset));
    }

    /**
     * Adds an array of bytes to the Bloom filter.
     *
     * @param bytes array of bytes to add to the Bloom filter.
     */
    private void add(String key, byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHashFunctions);
        for (int hash : hashes) {
            redisService.setBit(key, Math.abs(hash % sizeOfBloomFilter), true);
        }
    }

    /**
     * Adds all elements from a Collection to the Bloom filter.
     *
     * @param c Collection of elements.
     */
    public void addAll(String key, Collection<? extends E> c) {
        for (E element : c) {
            add(key, element);
        }
    }

    /**
     * Returns true if the element could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param element element to check.
     * @return true if the element could have been inserted into the Bloom filter.
     */
    public boolean contains(String key, E element) {
        return contains(key, element.toString().getBytes(charset));
    }

    /**
     * Returns true if the array of bytes could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param bytes array of bytes to check.
     * @return true if the array could have been inserted into the Bloom filter.
     */
    private boolean contains(String key, byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHashFunctions);
        for (int hash : hashes) {
            if (!redisService.getBit(key, Math.abs(hash % sizeOfBloomFilter))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if all the elements of a Collection could have been inserted
     * into the Bloom filter. Use getFalsePositiveProbability() to calculate the
     * probability of this being correct.
     *
     * @param c elements to check.
     * @return true if all the elements in c could have been inserted into the Bloom filter.
     */
    public boolean containsAll(String key, Collection<? extends E> c) {
        for (E element : c) {
            if (!contains(key, element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates digests based on the contents of an array of bytes and splits the result into 4-byte int's and store them in an array. The
     * digest function is called until the required number of int's are produced. For each call to digest a salt
     * is prepended to the data. The salt is increased by 1 for each call.
     *
     * @param data   specifies input data.
     * @param hashes number of hashes/int's to produce.
     * @return array of int-sized hashes
     */
    public static int[] createHashes(byte[] data, int hashes) {
        int[] result = new int[hashes];

        int k = 0;
        byte salt = 0;
        while (k < hashes) {
            byte[] digest;
            synchronized (digestFunction) {
                digestFunction.update(salt);
                salt++;
                digest = digestFunction.digest(data);
            }

            for (int i = 0; i < digest.length / 4 && k < hashes; i++) {
                int h = 0;
                for (int j = (i * 4); j < (i * 4) + 4; j++) {
                    h <<= 8;
                    h |= ((int) digest[j]) & 0xFF;
                }
                result[k] = h;
                k++;
            }
        }
        return result;
    }

    public int getSizeOfBloomFilter() {
        return this.sizeOfBloomFilter;
    }

    public int getExpectedNumberOfElements() {
        return this.expectedNumberOfFilterElements;
    }

    public int getNumberOfHashFunctions() {
        return this.numberOfHashFunctions;
    }

    /**
     * Compares the contents of two instances to see if they are equal.
     *
     * @param obj is the object to compare to.
     * @return True if the contents of the objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RedisBloomFilter<E> other = (RedisBloomFilter<E>) obj;
        if (this.sizeOfBloomFilter != other.sizeOfBloomFilter) {
            return false;
        }
        if (this.expectedNumberOfFilterElements != other.expectedNumberOfFilterElements) {
            return false;
        }
        if (this.numberOfHashFunctions != other.numberOfHashFunctions) {
            return false;
        }
        return true;
    }

    /**
     * Calculates a hash code for this class.
     *
     * @return hash code representing the contents of an instance of this class.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.sizeOfBloomFilter;
        hash = 61 * hash + this.expectedNumberOfFilterElements;
        hash = 61 * hash + this.numberOfHashFunctions;
        return hash;
    }
}
