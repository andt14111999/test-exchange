package com.exchangeengine.util;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Lớp tiện ích cung cấp các phương thức tạo và tùy chỉnh model trong unit test
 */
public class TestModelFactory {

  private TestModelFactory() {
    // Utility class không cần constructor
  }

  /**
   * Tùy chỉnh một đối tượng bằng cách áp dụng các giá trị từ map
   *
   * <p>
   * Sử dụng reflection để áp dụng các giá trị cho các trường tương ứng. Phương
   * thức này an toàn, bỏ qua các trường không tồn tại và kiểm tra kiểu dữ liệu
   * trước khi áp dụng.
   * </p>
   *
   * <p>
   * Ví dụ sử dụng:
   * </p>
   *
   * <pre>
   * AmmPosition position = AmmPositionFactory.createDefaultAmmPosition();
   * TestModelFactory.customize(position, Map.of(
   *     "poolPair", "ETH/USDT",
   *     "status", AmmPosition.STATUS_OPEN,
   *     "tickLowerIndex", 100));
   * </pre>
   *
   * @param <T>          Kiểu của đối tượng
   * @param model        Đối tượng cần tùy chỉnh
   * @param customFields Map các trường và giá trị tương ứng
   * @return Đối tượng đã được tùy chỉnh
   */
  public static <T> T customize(T model, Map<String, Object> customFields) {
    if (model == null || customFields == null) {
      return model;
    }

    @SuppressWarnings("unchecked")
    Class<T> modelClass = (Class<T>) model.getClass();

    for (Map.Entry<String, Object> entry : customFields.entrySet()) {
      String fieldName = entry.getKey();
      Object value = entry.getValue();

      try {
        Field field = modelClass.getDeclaredField(fieldName);
        field.setAccessible(true);

        // Kiểm tra kiểu dữ liệu và thực hiện chuyển đổi nếu cần
        Class<?> fieldType = field.getType();

        if (value == null) {
          field.set(model, null);
          continue;
        }

        // Xử lý các primitive types
        if (fieldType.isPrimitive()) {
          // Chuyển đổi số nguyên cho các kiểu primitive
          if (fieldType == int.class && value instanceof Number) {
            field.setInt(model, ((Number) value).intValue());
            continue;
          } else if (fieldType == long.class && value instanceof Number) {
            field.setLong(model, ((Number) value).longValue());
            continue;
          } else if (fieldType == float.class && value instanceof Number) {
            field.setFloat(model, ((Number) value).floatValue());
            continue;
          } else if (fieldType == double.class && value instanceof Number) {
            field.setDouble(model, ((Number) value).doubleValue());
            continue;
          } else if (fieldType == boolean.class && value instanceof Boolean) {
            field.setBoolean(model, (Boolean) value);
            continue;
          } else if (fieldType == byte.class && value instanceof Number) {
            field.setByte(model, ((Number) value).byteValue());
            continue;
          } else if (fieldType == short.class && value instanceof Number) {
            field.setShort(model, ((Number) value).shortValue());
            continue;
          } else if (fieldType == char.class && value instanceof Character) {
            field.setChar(model, (Character) value);
            continue;
          }
        }

        // Nếu không cần chuyển đổi đặc biệt, kiểm tra kiểu dữ liệu thông thường
        if (!fieldType.isInstance(value)) {
          throw new IllegalArgumentException(
              "Value for field '" + fieldName + "' is of type " + value.getClass().getName() +
                  " but field is of type " + fieldType.getName());
        }

        field.set(model, value);
      } catch (NoSuchFieldException e) {
        // Bỏ qua các field không tồn tại
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Cannot set field: " + fieldName, e);
      }
    }

    return model;
  }
}
