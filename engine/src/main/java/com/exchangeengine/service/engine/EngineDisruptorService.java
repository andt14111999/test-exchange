package com.exchangeengine.service.engine;

import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.TradeEvent;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.util.DaemonThreadFactory;
import com.exchangeengine.util.EnvManager;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

/**
 * Service manager Disruptor for engine events.
 */
public class EngineDisruptorService {
  private static final Logger logger = LoggerFactory.getLogger(EngineDisruptorService.class);
  private static final EnvManager envManager = EnvManager.getInstance();

  // Singleton instance
  private static volatile EngineDisruptorService instance;

  private final Disruptor<DisruptorEvent> disruptor;
  private final RingBuffer<DisruptorEvent> ringBuffer;
  private final String serviceName;

  /**
   * Lấy instance của EngineDisruptorService.
   *
   * @return Instance của EngineDisruptorService
   */
  public static synchronized EngineDisruptorService getInstance() {
    if (instance == null) {
      instance = createInstance();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(EngineDisruptorService testInstance) {
    instance = testInstance;
  }

  /**
   * Tạo instance mới của EngineDisruptorService với cấu hình từ environment.
   *
   * @return Instance mới của EngineDisruptorService
   */
  private static EngineDisruptorService createInstance() {
    logger.info("Initializing EngineDisruptorService...");

    // Lấy cấu hình từ environment
    try {
      int bufferSize = envManager.getInt("DISRUPTOR_BUFFER_SIZE", 4096);
      String serviceName = envManager.get("ENGINE_SERVICE_NAME", "engine-service");

      // Tạo DisruptorEventHandler
      DisruptorEventHandler eventHandler = new DisruptorEventHandler();

      // Tạo ThreadFactory với tên thread có ý nghĩa
      ThreadFactory threadFactory = new DaemonThreadFactory(serviceName, "disruptor");

      // Tạo Disruptor
      Disruptor<DisruptorEvent> disruptor = new Disruptor<>(
          new DisruptorEventFactory(),
          bufferSize,
          threadFactory,
          ProducerType.MULTI,
          new YieldingWaitStrategy());

      // Đăng ký event handler
      disruptor.handleEventsWith(eventHandler);

      // Khởi động disruptor
      disruptor.start();

      // Lấy ring buffer
      RingBuffer<DisruptorEvent> ringBuffer = disruptor.getRingBuffer();

      // Tạo instance mới
      EngineDisruptorService service = new EngineDisruptorService(disruptor, ringBuffer, serviceName);

      logger.info("EngineDisruptorService initialized with buffer size: {}", bufferSize);

      return service;
    } catch (Exception e) {
      logger.error("Error initializing EngineDisruptorService: {}", e.getMessage(), e);
      throw new RuntimeException("Cannot initialize EngineDisruptorService", e);
    }
  }

  /**
   * Constructor với Disruptor, RingBuffer và tên dịch vụ.
   * Private để đảm bảo Singleton pattern.
   */
  private EngineDisruptorService(Disruptor<DisruptorEvent> disruptor, RingBuffer<DisruptorEvent> ringBuffer,
      String serviceName) {
    this.disruptor = disruptor;
    this.ringBuffer = ringBuffer;
    this.serviceName = serviceName;
  }

  /**
   * Phương thức deposit mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là DEPOSIT
   */
  public void deposit(CoinDepositEvent coinDepositEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setCoinDepositEvent(coinDepositEvent);
    publishEvent(event);
  }

  /**
   * Phương thức withdraw mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là WITHDRAWAL
   */
  public void withdraw(CoinWithdrawalEvent coinWithdrawalEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setCoinWithdrawalEvent(coinWithdrawalEvent);

    publishEvent(event);
  }

  /**
   * Phương thức createCoinAccount mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là CREATE_COIN_ACCOUNT
   */
  public void createCoinAccount(AccountEvent accountEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(accountEvent);

    publishEvent(event);
  }

  /**
   * Phương thức ammPool mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là AMM_POOL
   */
  public void ammPool(AmmPoolEvent ammPoolEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setAmmPoolEvent(ammPoolEvent);

    publishEvent(event);
  }

  /**
   * Phương thức merchantEscrow mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là MERCHANT_ESCROW
   */
  public void merchantEscrow(MerchantEscrowEvent merchantEscrowEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setMerchantEscrowEvent(merchantEscrowEvent);

    publishEvent(event);
  }

  /**
   * Phương thức ammPosition mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là AMM_POSITION
   */
  public void ammPosition(AmmPositionEvent ammPositionEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setAmmPositionEvent(ammPositionEvent);

    publishEvent(event);
  }

  /**
   * Phương thức ammOrder mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là AMM_ORDER
   */
  public void ammOrder(AmmOrderEvent ammOrderEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setAmmOrderEvent(ammOrderEvent);

    publishEvent(event);
  }

  /**
   * Phương thức trade mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là TRADE
   */
  public void trade(TradeEvent tradeEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setTradeEvent(tradeEvent);

    publishEvent(event);
  }

  /**
   * Phương thức offer mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là OFFER
   */
  public void offer(OfferEvent offerEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setOfferEvent(offerEvent);

    publishEvent(event);
  }

  /**
   * Phương thức balancesLock mới nhận trực tiếp đối tượng DisruptorEvent
   * Đảm bảo rằng operationType được đặt là BALANCES_LOCK_CREATE hoặc
   * BALANCES_LOCK_RELEASE
   */
  public void balancesLock(BalancesLockEvent balancesLockEvent) {
    DisruptorEvent event = new DisruptorEvent();
    event.setBalancesLockEvent(balancesLockEvent);

    publishEvent(event);
  }

  /**
   * Phương thức publishEvent nhận trực tiếp đối tượng DisruptorEvent
   * và sao chép các thuộc tính của nó vào đối tượng trong RingBuffer
   */
  public void publishEvent(DisruptorEvent sourceEvent) {
    logger.debug("[{}] Publishing event={}", serviceName, sourceEvent);

    // Lấy sequence từ ring buffer - nếu buffer đầy, phương thức này sẽ block
    // cho đến khi có chỗ trống (nếu sử dụng BlockingWaitStrategy)
    long sequence = ringBuffer.next();
    try {
      // Lấy đối tượng từ RingBuffer
      DisruptorEvent bufferEvent = ringBuffer.get(sequence);

      // Sử dụng phương thức copyFrom của DisruptorEvent để sao chép các thuộc tính
      bufferEvent.copyFrom(sourceEvent);
    } finally {
      // Publish event để consumer có thể xử lý
      ringBuffer.publish(sequence);
    }
  }

  /**
   * Phương thức shutdown để tắt Disruptor
   */
  public void shutdown() {
    disruptor.shutdown();
    logger.info("[{}] Disruptor service shut down", serviceName);
  }

  /**
   * @return get name of service using Disruptor
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @return current size of ring buffer
   */
  public long getRemainingCapacity() {
    return ringBuffer.remainingCapacity();
  }
}
