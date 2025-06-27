package com.exchangeengine.model;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import com.exchangeengine.model.event.DisruptorEvent;

/**
 * Model chứa kết quả xử lý từ các processor
 */
public class ProcessResult {
  private final DisruptorEvent event;
  private Account account;
  private Account recipientAccount;
  private Account fiatAccount;
  private Account coinAccount;
  private AccountHistory accountHistory;
  private AccountHistory recipientAccountHistory;
  private AccountHistory fiatAccountHistory;
  private AccountHistory coinAccountHistory;
  private CoinDeposit deposit;
  private CoinWithdrawal withdrawal;
  private AmmPool ammPool;
  private MerchantEscrow merchantEscrow;
  private AmmPosition ammPosition;
  private AmmOrder ammOrder;
  private Offer offer;
  private Trade trade;
  private Account buyerAccount;
  private Account sellerAccount;
  private AccountHistory buyerAccountHistory;
  private AccountHistory sellerAccountHistory;
  private BalanceLock balanceLock;

  // Thêm các collections để hỗ trợ nhiều accounts và accountHistories
  private final Map<String, Account> accounts = new HashMap<>();
  private final List<AccountHistory> accountHistories = new ArrayList<>();
  private final List<Tick> ticks = new ArrayList<>();

  /**
   * Constructor với event
   *
   * @param event Event đã xử lý
   */
  public ProcessResult(DisruptorEvent event) {
    this.event = event;
  }

  /**
   * Tạo ProcessResult từ event với trạng thái thành công
   *
   * @param event Event đã xử lý
   * @return ProcessResult
   */
  public static ProcessResult success(DisruptorEvent event) {
    return new ProcessResult(event);
  }

  /**
   * Tạo ProcessResult từ event với trạng thái lỗi
   *
   * @param event        Event đã xử lý
   * @param errorMessage Thông báo lỗi
   * @return ProcessResult
   */
  public static ProcessResult error(DisruptorEvent event, String errorMessage) {
    ProcessResult result = new ProcessResult(event);
    event.setErrorMessage(errorMessage);
    return result;
  }

  /**
   * Lấy event
   *
   * @return Event
   */
  public DisruptorEvent getEvent() {
    return event;
  }

  /**
   * Lấy accountHistory
   *
   * @return Optional chứa AccountHistory nếu có
   */
  public Optional<AccountHistory> getAccountHistory() {
    return Optional.ofNullable(accountHistory);
  }

  /**
   * Đặt accountHistory
   *
   * @param accountHistory AccountHistory mới
   * @return this (để hỗ trợ method chaining)
   */
  public ProcessResult setAccountHistory(AccountHistory accountHistory) {
    this.accountHistory = accountHistory;
    return this;
  }

  /**
   * Lấy account
   *
   * @return Optional chứa Account nếu có
   */
  public Optional<Account> getAccount() {
    return Optional.ofNullable(account);
  }

  /**
   * Đặt account
   *
   * @param account Account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setAccount(Account account) {
    this.account = account;
    return this;
  }

  /**
   * Lấy recipient account
   *
   * @return Optional chứa recipient account nếu có
   */
  public Optional<Account> getRecipientAccount() {
    return Optional.ofNullable(recipientAccount);
  }

  /**
   * Đặt recipient account
   *
   * @param recipientAccount recipient account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setRecipientAccount(Account recipientAccount) {
    this.recipientAccount = recipientAccount;
    return this;
  }

  /**
   * Lấy fiat account
   *
   * @return Optional chứa Fiat Account nếu có
   */
  public Optional<Account> getFiatAccount() {
    return Optional.ofNullable(fiatAccount);
  }

  /**
   * Đặt fiat account
   *
   * @param fiatAccount Fiat Account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setFiatAccount(Account fiatAccount) {
    this.fiatAccount = fiatAccount;
    return this;
  }

  /**
   * Lấy coin account
   *
   * @return Optional chứa Coin Account nếu có
   */
  public Optional<Account> getCoinAccount() {
    return Optional.ofNullable(coinAccount);
  }

  /**
   * Đặt coin account
   *
   * @param coinAccount Coin Account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setCoinAccount(Account coinAccount) {
    this.coinAccount = coinAccount;
    return this;
  }

  /**
   * Lấy fiat account history
   *
   * @return Optional chứa Fiat Account History nếu có
   */
  public Optional<AccountHistory> getFiatAccountHistory() {
    return Optional.ofNullable(fiatAccountHistory);
  }

  /**
   * Đặt fiat account history
   *
   * @param fiatAccountHistory Fiat Account History cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setFiatAccountHistory(AccountHistory fiatAccountHistory) {
    this.fiatAccountHistory = fiatAccountHistory;
    return this;
  }

  /**
   * Lấy coin account history
   *
   * @return Optional chứa Coin Account History nếu có
   */
  public Optional<AccountHistory> getCoinAccountHistory() {
    return Optional.ofNullable(coinAccountHistory);
  }

  /**
   * Đặt coin account history
   *
   * @param coinAccountHistory Coin Account History cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setCoinAccountHistory(AccountHistory coinAccountHistory) {
    this.coinAccountHistory = coinAccountHistory;
    return this;
  }

  /**
   * Lấy balanceLock
   *
   * @return Optional chứa BalanceLock nếu có
   */
  public Optional<BalanceLock> getBalanceLock() {
    return Optional.ofNullable(balanceLock);
  }

  /**
   * Đặt balanceLock
   *
   * @param balanceLock BalanceLock cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setBalanceLock(BalanceLock balanceLock) {
    this.balanceLock = balanceLock;
    return this;
  }

  /**
   * Lấy recipient account history
   *
   * @return Optional chứa recipient account history nếu có
   */
  public Optional<AccountHistory> getRecipientAccountHistory() {
    return Optional.ofNullable(recipientAccountHistory);
  }

  /**
   * Đặt recipient account history
   *
   * @param recipientAccountHistory recipient account history cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setRecipientAccountHistory(AccountHistory recipientAccountHistory) {
    this.recipientAccountHistory = recipientAccountHistory;
    return this;
  }

  /**
   * Lấy account theo key
   *
   * @param key Khóa của account cần lấy
   * @return Optional chứa Account nếu có
   */
  public Optional<Account> getAccount(String key) {
    return Optional.ofNullable(accounts.get(key));
  }

  /**
   * Lấy deposit
   *
   * @return Optional chứa Deposit nếu có
   */
  public Optional<CoinDeposit> getDeposit() {
    return Optional.ofNullable(deposit);
  }

  /**
   * Đặt deposit
   *
   * @param deposit Deposit cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setDeposit(CoinDeposit deposit) {
    this.deposit = deposit;
    return this;
  }

  /**
   * Lấy withdrawal
   *
   * @return Optional chứa Withdrawal nếu có
   */
  public Optional<CoinWithdrawal> getWithdrawal() {
    return Optional.ofNullable(withdrawal);
  }

  /**
   * Đặt withdrawal
   *
   * @param withdrawal Withdrawal cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setWithdrawal(CoinWithdrawal withdrawal) {
    this.withdrawal = withdrawal;
    return this;
  }

  /**
   * Lấy AMM pool
   *
   * @return Optional chứa AMM pool nếu có
   */
  public Optional<AmmPool> getAmmPool() {
    return Optional.ofNullable(ammPool);
  }

  /**
   * Đặt AMM pool
   *
   * @param ammPool AMM pool cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setAmmPool(AmmPool ammPool) {
    this.ammPool = ammPool;
    return this;
  }

  /**
   * Lấy merchant escrow
   *
   * @return Optional chứa MerchantEscrow nếu có
   */
  public Optional<MerchantEscrow> getMerchantEscrow() {
    return Optional.ofNullable(merchantEscrow);
  }

  /**
   * Đặt merchant escrow
   *
   * @param merchantEscrow MerchantEscrow cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setMerchantEscrow(MerchantEscrow merchantEscrow) {
    this.merchantEscrow = merchantEscrow;
    return this;
  }

  /**
   * Thêm một account vào danh sách các accounts
   *
   * @param account Account cần thêm
   * @return this (để hỗ trợ method chaining)
   */
  public ProcessResult addAccount(Account account) {
    if (account != null) {
      this.accounts.put(account.getKey(), account);
    }
    return this;
  }

  /**
   * Chuyển đổi sang message JSON cho AmmPool
   *
   * @return Message JSON
   */
  public Map<String, Account> getAccounts() {
    return accounts;
  }

  /**
   * Thêm một accountHistory vào danh sách các accountHistories
   *
   * @param accountHistory AccountHistory cần thêm
   * @return this (để hỗ trợ method chaining)
   */
  public ProcessResult addAccountHistory(AccountHistory accountHistory) {
    if (accountHistory != null) {
      this.accountHistories.add(accountHistory);
    }
    return this;
  }

  /**
   * Lấy tất cả accountHistories
   *
   * @return List chứa các accountHistories đã thêm
   */
  public List<AccountHistory> getAccountHistories() {
    return accountHistories;
  }

  /**
   * Lấy ammPosition
   *
   * @return Optional chứa AmmPosition nếu có
   */
  public Optional<AmmPosition> getAmmPosition() {
    return Optional.ofNullable(ammPosition);
  }

  /**
   * Đặt ammPosition
   *
   * @param ammPosition AmmPosition mới
   * @return this (để hỗ trợ method chaining)
   */
  public ProcessResult setAmmPosition(AmmPosition ammPosition) {
    this.ammPosition = ammPosition;
    return this;
  }

  /**
   * Lấy AMM Order
   *
   * @return Optional chứa AMM Order nếu có
   */
  public Optional<AmmOrder> getAmmOrder() {
    return Optional.ofNullable(ammOrder);
  }

  /**
   * Đặt AMM Order
   *
   * @param ammOrder AMM Order cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setAmmOrder(AmmOrder ammOrder) {
    this.ammOrder = ammOrder;
    return this;
  }

  /**
   * Lấy offer
   *
   * @return Optional chứa Offer nếu có
   */
  public Optional<Offer> getOffer() {
    return Optional.ofNullable(offer);
  }

  /**
   * Đặt offer
   *
   * @param offer Offer cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setOffer(Offer offer) {
    this.offer = offer;
    return this;
  }

  /**
   * Lấy trade
   *
   * @return Optional chứa Trade nếu có
   */
  public Optional<Trade> getTrade() {
    return Optional.ofNullable(trade);
  }

  /**
   * Đặt trade
   *
   * @param trade Trade cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setTrade(Trade trade) {
    this.trade = trade;
    return this;
  }

  /**
   * Lấy buyer account
   *
   * @return Optional chứa buyer account nếu có
   */
  public Optional<Account> getBuyerAccount() {
    return Optional.ofNullable(buyerAccount);
  }

  /**
   * Đặt buyer account
   *
   * @param buyerAccount Buyer account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setBuyerAccount(Account buyerAccount) {
    this.buyerAccount = buyerAccount;
    return this;
  }

  /**
   * Lấy seller account
   *
   * @return Optional chứa seller account nếu có
   */
  public Optional<Account> getSellerAccount() {
    return Optional.ofNullable(sellerAccount);
  }

  /**
   * Đặt seller account
   *
   * @param sellerAccount Seller account cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setSellerAccount(Account sellerAccount) {
    this.sellerAccount = sellerAccount;
    return this;
  }

  /**
   * Lấy buyer account history
   *
   * @return Optional chứa buyer account history nếu có
   */
  public Optional<AccountHistory> getBuyerAccountHistory() {
    return Optional.ofNullable(buyerAccountHistory);
  }

  /**
   * Đặt buyer account history
   *
   * @param buyerAccountHistory Buyer account history cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setBuyerAccountHistory(AccountHistory buyerAccountHistory) {
    this.buyerAccountHistory = buyerAccountHistory;
    return this;
  }

  /**
   * Lấy seller account history
   *
   * @return Optional chứa seller account history nếu có
   */
  public Optional<AccountHistory> getSellerAccountHistory() {
    return Optional.ofNullable(sellerAccountHistory);
  }

  /**
   * Đặt seller account history
   *
   * @param sellerAccountHistory Seller account history cần đặt
   * @return ProcessResult instance for method chaining
   */
  public ProcessResult setSellerAccountHistory(AccountHistory sellerAccountHistory) {
    this.sellerAccountHistory = sellerAccountHistory;
    return this;
  }

  /**
   * Thêm một tick vào danh sách các ticks
   *
   * @param tick Tick cần thêm
   * @return this (để hỗ trợ method chaining)
   */
  public ProcessResult addTick(Tick tick) {
    if (tick != null) {
      this.ticks.add(tick);
    }
    return this;
  }

  /**
   * Lấy tất cả ticks
   *
   * @return List chứa các ticks đã thêm
   */
  public List<Tick> getTicks() {
    return ticks;
  }

  /**
   * Chuyển đổi sang message JSON cho AmmPool
   *
   * @return Message JSON
   */
  public Map<String, Object> toAmmPoolObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();
    Map<String, Object> objectMessageJson = ammPool.toMessageJson();

    messageJson.put("object", objectMessageJson);
    messageJson.put("inputEventId", event.getEventId() + "-" + ammPool.getPair());

    return messageJson;
  }

  /**
   * Chuyển đổi sang message JSON cho MerchantEscrow
   *
   * @return Message JSON
   */
  public Map<String, Object> toMerchantEscrowObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();
    Map<String, Object> objectMessageJson = merchantEscrow.toMessageJson();

    messageJson.put("object", objectMessageJson);
    messageJson.put("inputEventId", event.getEventId() + "-" + merchantEscrow.getIdentifier());

    return messageJson;
  }

  /**
   * Chuyển đổi sang message JSON cho AmmPosition
   *
   * @return Message JSON
   */
  public Map<String, Object> toAmmPositionObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();
    Map<String, Object> objectMessageJson = ammPosition.toMessageJson();

    messageJson.put("object", objectMessageJson);
    messageJson.put("inputEventId", event.getEventId() + "-" + ammPosition.getIdentifier());

    return messageJson;
  }

  /**
   * Tạo object message JSON cho AMM Order
   *
   * @return Map<String, Object> chứa thông tin cần gửi đi
   */
  public Map<String, Object> toAmmOrderObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();
    Map<String, Object> objectMessageJson = ammOrder.toMessageJson();

    messageJson.put("object", objectMessageJson);
    messageJson.put("inputEventId", event.getEventId() + "-" + ammOrder.getIdentifier());

    return messageJson;
  }

  /**
   * Tạo object message JSON cho Offer
   *
   * @return Map<String, Object> chứa thông tin cần gửi đi
   */
  public Map<String, Object> toOfferObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();

    if (offer != null) {
      messageJson.put("object", offer.toMessageJson());
      messageJson.put("inputEventId", event.getEventId() + "-" + offer.getIdentifier());
    }

    return messageJson;
  }

  /**
   * Tạo object message JSON cho Trade
   *
   * @return Map<String, Object> chứa thông tin cần gửi đi
   */
  public Map<String, Object> toTradeObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();

    if (trade != null) {
      messageJson.put("object", trade.toMessageJson());
      messageJson.put("inputEventId", event.getEventId() + "-" + trade.getIdentifier());
    }

    return messageJson;
  }

  /**
   * Tạo JSON object cho BalanceLock để gửi qua Kafka
   *
   * @return Map chứa dữ liệu JSON
   */
  public Map<String, Object> toBalanceLockObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();

    if (balanceLock != null) {
      messageJson.put("object", balanceLock.toMessageJson());
      messageJson.put("inputEventId", event.getEventId() + "-" + balanceLock.getIdentifier());
    }

    return messageJson;
  }

  /**
   * Tạo JSON object cho CoinWithdrawal để gửi qua Kafka
   *
   * @return Map chứa dữ liệu JSON
   */
  public Map<String, Object> toCoinWithdrawalObjectMessageJson() {
    Map<String, Object> messageJson = event.toDisruptorMessageJson();

    if (withdrawal != null) {
      messageJson.put("object", withdrawal.toMessageJson());
      messageJson.put("inputEventId", event.getEventId() + "-" + withdrawal.getIdentifier());
    }

    return messageJson;
  }
}
