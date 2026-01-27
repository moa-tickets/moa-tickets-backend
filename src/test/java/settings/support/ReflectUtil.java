package settings.support;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.hibernate.internal.util.ReflectHelper.findField;

public class ReflectUtil {
    private ReflectUtil() {}

    public static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            Object v = f.get(target);
            return type.cast(v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAtomicLong(Object target, String fieldName, long value) {
        AtomicLong al = getField(target, fieldName, AtomicLong.class);
        al.set(value);
    }
}
