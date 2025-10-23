package com.adoption.common.util;

import java.util.UUID;

public class IdGenerator {
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
