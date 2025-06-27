package com.exchangeengine;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.exchangeengine.bootstrap.EngineServiceMain;
import com.exchangeengine.bootstrap.ServiceInitializer;
import com.exchangeengine.util.EnvManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MainTest {

  private MockedStatic<ServiceInitializer> mockedServiceInitializer;
  private MockedStatic<EngineServiceMain> mockedEngineServiceMain;
  private MockedStatic<EnvManager> mockedEnvManager;
  private MockedStatic<Runtime> mockedRuntime;

  private EnvManager mockEnvManager;
  private Runtime mockRuntime;

  @BeforeEach
  void setUp() {
    // Mock EnvManager
    mockEnvManager = mock(EnvManager.class);
    when(mockEnvManager.getEnvironment()).thenReturn("test");

    mockedEnvManager = mockStatic(EnvManager.class);
    mockedEnvManager.when(EnvManager::getInstance).thenReturn(mockEnvManager);

    // Mock ServiceInitializer (static methods)
    mockedServiceInitializer = mockStatic(ServiceInitializer.class);

    // Mock EngineServiceMain (static methods)
    mockedEngineServiceMain = mockStatic(EngineServiceMain.class);

    // Mock Runtime
    mockRuntime = mock(Runtime.class);
    mockedRuntime = mockStatic(Runtime.class);
    mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
  }

  @AfterEach
  void tearDown() {
    if (mockedServiceInitializer != null) {
      mockedServiceInitializer.close();
    }

    if (mockedEngineServiceMain != null) {
      mockedEngineServiceMain.close();
    }

    if (mockedEnvManager != null) {
      mockedEnvManager.close();
    }

    if (mockedRuntime != null) {
      mockedRuntime.close();
    }
  }

  @Test
  @DisplayName("Test constructor can be called")
  void testConstructor() {
    // Tạo instance mới để đảm bảo constructor được gọi
    new Main();
  }

  @Test
  @DisplayName("Test exception handling when ServiceInitializer.initialize() throws exception")
  void testMainHandlesInitializeException() {
    // Giả lập exception từ ServiceInitializer.initialize()
    Exception testException = new RuntimeException("Test initialization error");
    mockedServiceInitializer.when(ServiceInitializer::initialize).thenThrow(testException);

    // Act - gọi main
    Main.main(new String[] {});

    // Assert - xác nhận initialize được gọi và EngineServiceMain.start không được
    // gọi
    mockedServiceInitializer.verify(ServiceInitializer::initialize, times(1));
    mockedEngineServiceMain.verify(EngineServiceMain::start, never());
  }

  @Test
  @DisplayName("Happy path - Kiểm tra main gọi đúng các phương thức cần thiết")
  void testMainHappyPath() {
    // 1. Cài đặt doAnswer cho addShutdownHook để gọi latch.countDown() ngay lập tức
    doAnswer(invocation -> {
      Thread thread = invocation.getArgument(0);
      // Chạy shutdown hook ngay lập tức để kết thúc sớm
      thread.run();
      return null;
    }).when(mockRuntime).addShutdownHook(any(Thread.class));

    // Act - gọi main
    Main.main(new String[] {});

    // Assert - xác nhận các phương thức đã được gọi đúng thứ tự
    // 1. ServiceInitializer.initialize được gọi
    mockedServiceInitializer.verify(ServiceInitializer::initialize, times(1));

    // 2. EngineServiceMain.start được gọi
    mockedEngineServiceMain.verify(EngineServiceMain::start, times(1));

    // 3. Đảm bảo addShutdownHook được gọi
    verify(mockRuntime, times(1)).addShutdownHook(any(Thread.class));

    // 4. Đảm bảo EngineServiceMain.stop được gọi (từ shutdown hook)
    mockedEngineServiceMain.verify(EngineServiceMain::stop, times(1));

    // 5. Đảm bảo ServiceInitializer.shutdown được gọi (từ shutdown hook)
    mockedServiceInitializer.verify(ServiceInitializer::shutdown, times(1));
  }

  @Test
  @DisplayName("Kiểm tra shutdown hook gọi đúng phương thức dừng")
  void testShutdownHook() {
    // Capture shutdown hook và chạy nó
    doAnswer(invocation -> {
      // Lấy thread shutdown hook
      Thread shutdownHook = invocation.getArgument(0);

      // Chạy shutdown hook
      shutdownHook.run();

      // Xác nhận các phương thức dừng được gọi đúng thứ tự
      mockedEngineServiceMain.verify(EngineServiceMain::stop, times(1));
      mockedServiceInitializer.verify(ServiceInitializer::shutdown, times(1));

      return null;
    }).when(mockRuntime).addShutdownHook(any(Thread.class));

    // Act - tạo shutdown hook thông qua main
    Main.main(new String[] {});

    // Assert - xác nhận hook đã được thêm
    verify(mockRuntime, times(1)).addShutdownHook(any(Thread.class));
  }
}
