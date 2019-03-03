package com.lizhi.utils;

/**
 * @author https://github.com/lizhixiong1994
 * @Date 2019-02-28
 */
public class RedisUtil {

    /*
     *         int[] ints = new int[2];
     *         ints[0] = 1;
     *         ints[1] = 0;
     *
     *         int[] ints2 = new int[2];
     *         ints2[0] = 0;
     *         ints2[1] = 1;
     *         int[] intersection = getIntersection(ints, ints2);
     *         int[] union = getUnion(ints, ints2);
     *
     * result : intersection [0][0]
     *        : union        [1][1]
     */

    public static int[] getIntersection(int[]... ints) {
        int len = ints[0].length;
        for (int[] ints1 : ints) {
            if (len != ints1.length) {
                throw new IllegalArgumentException("长度不一致");
            }
        }
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = getIntersection(i, ints);
        }
        return result;
    }

    public static int[] getUnion(int[]... ints) {
        int len = ints[0].length;
        for (int[] ints1 : ints) {
            if (len != ints1.length) {
                throw new IllegalArgumentException("长度不一致");
            }
        }
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = getUnion(i, ints);
        }
        return result;
    }

    private static int getIntersection(int index, int[][] ints) {
        int reuslt = 0xffff;
        for (int[] in : ints) {
            reuslt &= in[index];
        }
        return reuslt;
    }

    private static int getUnion(int index, int[][] ints) {
        int reuslt = 0x0000;
        for (int[] in : ints) {
            reuslt |= in[index];
        }
        return reuslt;
    }

}
