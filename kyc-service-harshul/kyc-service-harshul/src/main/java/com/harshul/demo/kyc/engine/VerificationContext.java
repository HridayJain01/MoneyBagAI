package com.harshul.demo.kyc.engine;
import java.util.HashMap;
import java.util.Map;

public final class VerificationContext {

    private final VerificationRequest request;

    private final Map<Class<?>, Object> values = new HashMap<>();

    private VerificationContext(VerificationRequest request) {
        this.request = request;
    }

    public static VerificationContext of(VerificationRequest request) {
        return new VerificationContext(request);
    }

    public VerificationRequest request() {
        return request;
    }

    public <T> void put(T value) {
        values.put(value.getClass(), value);
    }

    public <T> T get(Class<T> type) {
        Object value = values.get(type);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
}