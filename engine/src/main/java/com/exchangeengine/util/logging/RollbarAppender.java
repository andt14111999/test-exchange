package com.exchangeengine.util.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;

import com.exchangeengine.util.EnvManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Logback appender cho Rollbar.
 * Gửi các log error, warning và critical đến Rollbar.
 */
public class RollbarAppender extends AppenderBase<ILoggingEvent> {
  private static final EnvManager envManager = EnvManager.getInstance();
  private Rollbar rollbar;
  private String accessToken;
  private String environment;

  @Override
  public void start() {
    if (this.accessToken == null || this.accessToken.isEmpty()) {
      addError("Không thể khởi tạo RollbarAppender: accessToken không được cung cấp");
      return;
    }

    // Khởi tạo environment dựa trên EnvManager
    if (this.environment == null || this.environment.isEmpty()) {
      this.environment = envManager.isProduction() ? "production" : "development";
    }

    Config config = ConfigBuilder.withAccessToken(this.accessToken)
        .environment(this.environment)
        .codeVersion("1.0.0")
        .build();

    this.rollbar = Rollbar.init(config);
    super.start();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (!isStarted() || event == null) {
      return;
    }

    // Chỉ gửi log levels ERROR, WARN và cấp cao hơn đến Rollbar
    if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
      String message = event.getFormattedMessage();

      Map<String, Object> custom = new HashMap<>();
      custom.put("logger_name", event.getLoggerName());
      custom.put("thread_name", event.getThreadName());
      custom.put("level", event.getLevel().toString());
      custom.put("mdc", event.getMDCPropertyMap());

      try {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null && throwableProxy instanceof ThrowableProxy) {
          Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
          rollbar.error(throwable, custom, message);
        } else {
          if (event.getLevel() == Level.ERROR) {
            rollbar.error(message, custom);
          } else if (event.getLevel() == Level.WARN) {
            rollbar.warning(message, custom);
          } else {
            rollbar.info(message, custom);
          }
        }
      } catch (Exception e) {
        // Bắt exception từ Rollbar API để tránh retry
        // Chỉ log một lần vào System.err
        System.err.println("Rollbar API failed: " + e.getMessage());
      }
    }
  }

  @Override
  public void stop() {
    try {
      if (this.rollbar != null) {
        this.rollbar.close(true);
      }
    } catch (Exception e) {
      // Không dùng addError để tránh spam log
      System.err.println("Lỗi khi đóng kết nối Rollbar trong appender: " + e.getMessage());
    } finally {
      super.stop();
    }
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }
}
