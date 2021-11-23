package net.homework.blockchain.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ByteUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static <T> T fromBytes(byte[] bytes, T t) {
        try {
            return (T) MAPPER.readValue(bytes, t.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T fromJson(String json, T t) {
        try {
            return (T) MAPPER.readValue(json, t.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] toBytes(Object o) {
        try {
            return MAPPER.writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isZero(byte[] array) {
        int sum = 0;
        for (byte b : array) {
            sum |= b;
        }
        return sum == 0;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try {
            s.write(a);
            s.write(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s.toByteArray();
    }

    public static byte[] removeLeadingZero(byte[] input) {
        if (input[0] == 0) {
            return Arrays.copyOfRange(input, 1, input.length);
        } else {
            return input;
        }
    }
}
