package ir.omid.smartteacher.ai.internal;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * ساخت نمونه ساختگی برای خروجی ساختاریافته در حالت Mock.
 *
 * <p>بدون فراخوانی شبکه و بدون حدس شکل JSON Provider واقعی.
 * برای recordها سازنده فشرده؛ برای کلاس‌های ساده سازنده بدون آرگومان.
 */
final class StructuredOutputFixtures {

    private StructuredOutputFixtures() {
    }

    @SuppressWarnings("unchecked")
    static <T> T createSample(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("نوع پاسخ نمی‌تواند null باشد");
        }
        if (type == String.class) {
            return (T) "نمونه-ساختگی";
        }
        if (type == Map.class) {
            Map<String, Object> map = new HashMap<>();
            map.put("mock", true);
            map.put("score", BigDecimal.ONE);
            return (T) map;
        }
        if (type.isRecord()) {
            return createRecordSample(type);
        }
        try {
            Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "نمی‌توان نمونه ساختگی برای " + type.getName() + " ساخت", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createRecordSample(Class<T> type) {
        try {
            Constructor<?> ctor = type.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Class<?>[] params = ctor.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                args[i] = defaultValue(params[i]);
            }
            return (T) ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "نمی‌توان record ساختگی برای " + type.getName() + " ساخت", e);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return true;
        }
        if (type == int.class || type == Integer.class) {
            return 1;
        }
        if (type == long.class || type == Long.class) {
            return 1L;
        }
        if (type == double.class || type == Double.class) {
            return 1.0d;
        }
        if (type == float.class || type == Float.class) {
            return 1.0f;
        }
        if (type == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (type == String.class) {
            return "نمونه";
        }
        if (type == java.util.List.class) {
            return java.util.List.of("نمونه");
        }
        if (type.isRecord()) {
            return createRecordSample(type);
        }
        return null;
    }
}
