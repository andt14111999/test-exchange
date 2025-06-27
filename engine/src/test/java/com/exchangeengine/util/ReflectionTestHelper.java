package com.exchangeengine.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Lớp tiện ích hỗ trợ các thao tác phản chiếu (reflection) trong test
 */
public class ReflectionTestHelper {

  /**
   * Lấy và set quyền truy cập cho một phương thức private
   *
   * @param targetClass Lớp chứa phương thức
   * @param methodName  Tên phương thức
   * @param paramTypes  Mảng các kiểu tham số của phương thức
   * @return Đối tượng Method đã được set accessible
   * @throws NoSuchMethodException Nếu phương thức không tồn tại
   */
  public static Method getAccessibleMethod(Class<?> targetClass, String methodName, Class<?>... paramTypes)
      throws NoSuchMethodException {
    Method method = targetClass.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method;
  }

  /**
   * Lấy và set quyền truy cập cho một trường private
   *
   * @param targetClass Lớp chứa trường
   * @param fieldName   Tên trường
   * @return Đối tượng Field đã được set accessible
   * @throws NoSuchFieldException Nếu trường không tồn tại
   */
  public static Field getAccessibleField(Class<?> targetClass, String fieldName) throws NoSuchFieldException {
    Field field = targetClass.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }

  /**
   * Gọi một phương thức private
   *
   * @param method   Đối tượng Method (đã được set accessible)
   * @param instance Đối tượng thực thể cần gọi phương thức, null nếu là static
   *                 method
   * @param args     Các tham số cần truyền vào phương thức
   * @return Kết quả trả về từ phương thức
   * @throws Exception Nếu gọi phương thức thất bại
   */
  public static Object invokeMethod(Method method, Object instance, Object... args) throws Exception {
    return method.invoke(instance, args);
  }

  /**
   * Gọi phương thức private bằng tên và tham số
   *
   * @param targetClass Lớp chứa phương thức
   * @param instance    Đối tượng thực thể cần gọi phương thức, null nếu là static
   *                    method
   * @param methodName  Tên phương thức
   * @param paramTypes  Mảng các kiểu tham số của phương thức
   * @param args        Các tham số cần truyền vào phương thức
   * @return Kết quả trả về từ phương thức
   * @throws Exception Nếu gọi phương thức thất bại
   */
  public static Object invokeMethod(Class<?> targetClass, Object instance, String methodName,
      Class<?>[] paramTypes, Object... args) throws Exception {
    Method method = getAccessibleMethod(targetClass, methodName, paramTypes);
    return invokeMethod(method, instance, args);
  }

  /**
   * Đọc giá trị từ một trường private
   *
   * @param field    Đối tượng Field (đã được set accessible)
   * @param instance Đối tượng thực thể cần đọc giá trị, null nếu là static field
   * @return Giá trị của trường
   * @throws IllegalAccessException Nếu không thể truy cập trường
   */
  public static Object getFieldValue(Field field, Object instance) throws IllegalAccessException {
    return field.get(instance);
  }

  /**
   * Đọc giá trị từ một trường private bằng tên
   *
   * @param targetClass Lớp chứa trường
   * @param instance    Đối tượng thực thể cần đọc giá trị, null nếu là static
   *                    field
   * @param fieldName   Tên trường
   * @return Giá trị của trường
   * @throws NoSuchFieldException   Nếu trường không tồn tại
   * @throws IllegalAccessException Nếu không thể truy cập trường
   */
  public static Object getFieldValue(Class<?> targetClass, Object instance, String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = getAccessibleField(targetClass, fieldName);
    return getFieldValue(field, instance);
  }

  /**
   * Gán giá trị cho một trường private
   *
   * @param field    Đối tượng Field (đã được set accessible)
   * @param instance Đối tượng thực thể cần gán giá trị, null nếu là static field
   * @param value    Giá trị mới cần gán
   * @throws IllegalAccessException Nếu không thể truy cập trường
   */
  public static void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
    field.set(instance, value);
  }

  /**
   * Gán giá trị cho một trường private bằng tên
   *
   * @param targetClass Lớp chứa trường
   * @param instance    Đối tượng thực thể cần gán giá trị, null nếu là static
   *                    field
   * @param fieldName   Tên trường
   * @param value       Giá trị mới cần gán
   * @throws NoSuchFieldException   Nếu trường không tồn tại
   * @throws IllegalAccessException Nếu không thể truy cập trường
   */
  public static void setFieldValue(Class<?> targetClass, Object instance, String fieldName, Object value)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = getAccessibleField(targetClass, fieldName);
    setFieldValue(field, instance, value);
  }
}
