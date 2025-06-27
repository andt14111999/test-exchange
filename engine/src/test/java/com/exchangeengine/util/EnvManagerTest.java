package com.exchangeengine.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class EnvManagerTest {

  @Mock
  private Dotenv dotenv;

  private EnvManager envManager;

  // Lưu giá trị ban đầu của System properties
  private String originalIsTest;
  private String originalAppEnv;

  @BeforeEach
  void setUp() throws Exception {
    // Lưu lại các thuộc tính hệ thống ban đầu
    originalIsTest = System.getProperty("isTest");
    originalAppEnv = System.getProperty("APP_ENV");

    // Reset the singleton instance before each test
    Field instanceField = EnvManager.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Tạo instance bằng reflection vì constructor là private
    Constructor<EnvManager> constructor = EnvManager.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    envManager = constructor.newInstance();

    // Inject our mocked dotenv
    Field dotenvField = EnvManager.class.getDeclaredField("dotenv");
    dotenvField.setAccessible(true);
    dotenvField.set(envManager, dotenv);

    // Make the environment field accessible
    Field environmentField = EnvManager.class.getDeclaredField("environment");
    environmentField.setAccessible(true);
    environmentField.set(envManager, "dev");

    // Set our instance as the singleton
    instanceField.set(null, envManager);
  }

  @AfterEach
  void tearDown() {
    // Khôi phục các thuộc tính hệ thống về giá trị ban đầu
    if (originalIsTest != null) {
      System.setProperty("isTest", originalIsTest);
    } else {
      System.clearProperty("isTest");
    }

    if (originalAppEnv != null) {
      System.setProperty("APP_ENV", originalAppEnv);
    } else {
      System.clearProperty("APP_ENV");
    }
  }

  @Test
  @DisplayName("getInstance should return singleton instance")
  void getInstance_ShouldReturnSingletonInstance() {
    EnvManager instance1 = EnvManager.getInstance();
    EnvManager instance2 = EnvManager.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("get should return environment variable value")
  void get_ShouldReturnEnvironmentVariableValue() {
    when(dotenv.get("TEST_KEY")).thenReturn("test_value");

    String result = envManager.get("TEST_KEY");

    assertEquals("test_value", result);
    verify(dotenv).get("TEST_KEY");
  }

  @Test
  @DisplayName("get with default should return value when variable exists")
  void getWithDefault_ShouldReturnValue_WhenVariableExists() {
    when(dotenv.get("TEST_KEY", "default_value")).thenReturn("test_value");

    String result = envManager.get("TEST_KEY", "default_value");

    assertEquals("test_value", result);
    verify(dotenv).get("TEST_KEY", "default_value");
  }

  @Test
  @DisplayName("getInt should return int value when variable exists")
  void getInt_ShouldReturnIntValue_WhenVariableExists() {
    when(dotenv.get("INT_KEY")).thenReturn("123");

    int result = envManager.getInt("INT_KEY", 0);

    assertEquals(123, result);
    verify(dotenv).get("INT_KEY");
  }

  @Test
  @DisplayName("getInt should return default value when variable doesn't exist")
  void getInt_ShouldReturnDefaultValue_WhenVariableDoesntExist() {
    when(dotenv.get("INT_KEY")).thenReturn(null);

    int result = envManager.getInt("INT_KEY", 456);

    assertEquals(456, result);
    verify(dotenv).get("INT_KEY");
  }

  @Test
  @DisplayName("getInt should return default value when variable is not a number")
  void getInt_ShouldReturnDefaultValue_WhenVariableIsNotANumber() {
    when(dotenv.get("INT_KEY")).thenReturn("not_a_number");

    int result = envManager.getInt("INT_KEY", 789);

    assertEquals(789, result);
    verify(dotenv).get("INT_KEY");
  }

  @Test
  @DisplayName("getBoolean should return boolean value when variable exists")
  void getBoolean_ShouldReturnBooleanValue_WhenVariableExists() {
    when(dotenv.get("BOOL_KEY")).thenReturn("true");

    boolean result = envManager.getBoolean("BOOL_KEY", false);

    assertTrue(result);
    verify(dotenv).get("BOOL_KEY");
  }

  @Test
  @DisplayName("getBoolean should return default value when variable doesn't exist")
  void getBoolean_ShouldReturnDefaultValue_WhenVariableDoesntExist() {
    when(dotenv.get("BOOL_KEY")).thenReturn(null);

    boolean result = envManager.getBoolean("BOOL_KEY", true);

    assertTrue(result);
    verify(dotenv).get("BOOL_KEY");
  }

  @Test
  @DisplayName("Environment detection methods should work correctly")
  void environmentDetectionMethods_ShouldWorkCorrectly() throws Exception {
    // Test for development environment
    Field environmentField = EnvManager.class.getDeclaredField("environment");
    environmentField.setAccessible(true);
    environmentField.set(envManager, "development");

    assertTrue(envManager.isDevelopment());
    assertFalse(envManager.isStaging());
    assertFalse(envManager.isProduction());
    assertFalse(envManager.isTest());

    // Test for staging environment
    environmentField.set(envManager, "staging");

    assertFalse(envManager.isDevelopment());
    assertTrue(envManager.isStaging());
    assertFalse(envManager.isProduction());
    assertFalse(envManager.isTest());

    // Test for production environment
    environmentField.set(envManager, "production");

    assertFalse(envManager.isDevelopment());
    assertFalse(envManager.isStaging());
    assertTrue(envManager.isProduction());
    assertFalse(envManager.isTest());

    // Test for test environment
    environmentField.set(envManager, "test");

    assertFalse(envManager.isDevelopment());
    assertFalse(envManager.isStaging());
    assertFalse(envManager.isProduction());
    assertTrue(envManager.isTest());
  }

  @Test
  @DisplayName("isTest should return true when environment is test")
  void isTest_ShouldReturnTrue_WhenEnvironmentIsTest() throws Exception {
    // Thiết lập môi trường là test
    Field environmentField = EnvManager.class.getDeclaredField("environment");
    environmentField.setAccessible(true);
    environmentField.set(envManager, "test");

    assertTrue(envManager.isTest());
  }

  @Test
  @DisplayName("Constructor should detect test environment with isTest property")
  void constructor_ShouldDetectTestEnvironment_WithIsTestProperty() throws Exception {
    // Thiết lập System.property("isTest")
    System.setProperty("isTest", "true");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockTestDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("src/test/java/com/exchangeengine/config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env.test")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockTestDotenv);

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra mối trường là test
      assertEquals("test", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("src/test/java/com/exchangeengine/config");
      verify(mockBuilder).filename(".env.test");
    }
  }

  @Test
  @DisplayName("Constructor should detect test environment with APP_ENV property")
  void constructor_ShouldDetectTestEnvironment_WithAppEnvProperty() throws Exception {
    // Thiết lập System.property("APP_ENV")
    System.setProperty("APP_ENV", "test");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockTestDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("src/test/java/com/exchangeengine/config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env.test")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockTestDotenv);

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra mối trường là test
      assertEquals("test", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("src/test/java/com/exchangeengine/config");
      verify(mockBuilder).filename(".env.test");
    }
  }

  @Test
  @DisplayName("Constructor should configure for non-test environment")
  void constructor_ShouldConfigureForNonTestEnvironment() throws Exception {
    // Đảm bảo không có System property nào cho test
    System.clearProperty("isTest");
    System.clearProperty("APP_ENV");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockProdDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("./config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockProdDotenv);

      // Giả lập khi gọi get("APP_ENV", "development")
      when(mockProdDotenv.get("APP_ENV", "development")).thenReturn("development");

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra môi trường là development
      assertEquals("development", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("./config");
      verify(mockBuilder).filename(".env");
      verify(mockProdDotenv).get("APP_ENV", "development");
    }
  }

  @Test
  @DisplayName("setTestEnvironment should set test properties and reset instance")
  void setTestEnvironment_ShouldSetTestPropertiesAndResetInstance() throws Exception {
    // Lấy EnvManager instance ban đầu
    EnvManager originalInstance = EnvManager.getInstance();

    // Gọi setTestEnvironment
    EnvManager.setTestEnvironment();

    // Kiểm tra System properties đã được thiết lập
    assertEquals("true", System.getProperty("isTest"));
    assertEquals("test", System.getProperty("APP_ENV"));

    // Kiểm tra instance đã bị reset (now null)
    Field instanceField = EnvManager.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    assertNull(instanceField.get(null));

    // Khi gọi lại getInstance sẽ tạo ra instance mới khác với instance ban đầu
    EnvManager newInstance = EnvManager.getInstance();
    assertNotSame(originalInstance, newInstance);
  }

  @Test
  @DisplayName("Constructor should detect test environment with APP_ENV property in different case")
  void constructor_ShouldDetectTestEnvironment_WithAppEnvPropertyDifferentCase() throws Exception {
    // Thiết lập System.property("APP_ENV") với giá trị "TEST" (chữ in hoa)
    System.setProperty("APP_ENV", "TEST");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockTestDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("src/test/java/com/exchangeengine/config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env.test")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockTestDotenv);

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra môi trường là test
      assertEquals("test", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("src/test/java/com/exchangeengine/config");
      verify(mockBuilder).filename(".env.test");
    }
  }

  @Test
  @DisplayName("Constructor should detect test environment with both conditions")
  void constructor_ShouldDetectTestEnvironment_WithBothConditions() throws Exception {
    // Thiết lập cả hai property
    System.setProperty("isTest", "true");
    System.setProperty("APP_ENV", "production"); // Không phải "test" để đảm bảo điều kiện thứ hai không ảnh hưởng

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockTestDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("src/test/java/com/exchangeengine/config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env.test")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockTestDotenv);

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra môi trường là test
      assertEquals("test", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("src/test/java/com/exchangeengine/config");
      verify(mockBuilder).filename(".env.test");
    }
  }

  @Test
  @DisplayName("Constructor should not detect test environment when APP_ENV is not test")
  void constructor_ShouldNotDetectTestEnvironment_WhenAppEnvIsNotTest() throws Exception {
    // Thiết lập System.property("APP_ENV") với giá trị không phải test
    System.setProperty("APP_ENV", "production");
    System.clearProperty("isTest");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockProdDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("./config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockProdDotenv);

      // Giả lập khi gọi get("APP_ENV", "development")
      when(mockProdDotenv.get("APP_ENV", "development")).thenReturn("production");

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra môi trường không phải test
      assertEquals("production", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("./config");
      verify(mockBuilder).filename(".env");
    }
  }

  @Test
  @DisplayName("Constructor should evaluate both conditions for test environment when first is false")
  void constructor_ShouldEvaluateSecondCondition_WhenFirstIsFalse() throws Exception {
    // Xóa System.property("isTest") và thiết lập "APP_ENV"
    System.clearProperty("isTest");
    System.setProperty("APP_ENV", "test");

    try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class)) {
      DotenvBuilder mockBuilder = mock(DotenvBuilder.class);
      Dotenv mockTestDotenv = mock(Dotenv.class);

      // Tạo chuỗi mock cho các method call
      when(Dotenv.configure()).thenReturn(mockBuilder);
      when(mockBuilder.directory("src/test/java/com/exchangeengine/config")).thenReturn(mockBuilder);
      when(mockBuilder.filename(".env.test")).thenReturn(mockBuilder);
      when(mockBuilder.ignoreIfMissing()).thenReturn(mockBuilder);
      when(mockBuilder.systemProperties()).thenReturn(mockBuilder);
      when(mockBuilder.load()).thenReturn(mockTestDotenv);

      // Reset singleton và tạo mới EnvManager
      Field instanceField = EnvManager.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      EnvManager instance = EnvManager.getInstance();

      // Kiểm tra môi trường là test
      assertEquals("test", instance.getEnvironment());

      // Xác nhận rằng việc cấu hình Dotenv được gọi với các tham số đúng
      verify(mockBuilder).directory("src/test/java/com/exchangeengine/config");
      verify(mockBuilder).filename(".env.test");
    }
  }

  @Test
  @DisplayName("Singletons should not be reinitialized after shutdown")
  void singletons_ShouldNotBeReinitialized_AfterShutdown() throws Exception {
    // Reset singleton và tạo mới EnvManager
    Field instanceField = EnvManager.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Lấy instance lần đầu
    EnvManager instance1 = EnvManager.getInstance();

    // Thiết lập lại môi trường test
    EnvManager.setTestEnvironment();

    // Lấy instance lần thứ hai (sau khi reset)
    EnvManager instance2 = EnvManager.getInstance();

    // Kiểm tra hai instance khác nhau
    assertNotSame(instance1, instance2);

    // Kiểm tra môi trường của instance mới là test
    assertTrue(instance2.isTest());
  }
}
