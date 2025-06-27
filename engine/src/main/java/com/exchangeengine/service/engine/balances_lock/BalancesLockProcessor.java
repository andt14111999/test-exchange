package com.exchangeengine.service.engine.balances_lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.BalanceLockCache;

/**
 * Xử lý các sự kiện khóa và giải phóng số dư
 */
public class BalancesLockProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BalancesLockProcessor.class);
    
    private final DisruptorEvent event;
    private final AccountCache accountCache;
    private final BalanceLockCache balanceLockCache;
    private final ProcessResult result;
    
    /**
     * Khởi tạo processor với event
     * 
     * @param event Event cần xử lý
     */
    public BalancesLockProcessor(DisruptorEvent event) {
        this.event = event;
        this.accountCache = AccountCache.getInstance();
        this.balanceLockCache = BalanceLockCache.getInstance();
        this.result = new ProcessResult(event);
    }
    
    /**
     * Xử lý event khóa hoặc giải phóng số dư
     * 
     * @return ProcessResult chứa kết quả xử lý
     */
    public ProcessResult process() {
        BalancesLockEvent lockEvent = event.getBalancesLockEvent();
        
        try {
            if (OperationType.BALANCES_LOCK_CREATE.isEqualTo(lockEvent.getOperationType().getValue())) {
                return processCreateLock(lockEvent);
            } else if (OperationType.BALANCES_LOCK_RELEASE.isEqualTo(lockEvent.getOperationType().getValue())) {
                return processReleaseLock(lockEvent);
            } else {
                String errorMessage = "OperationType không được hỗ trợ: " + lockEvent.getOperationType().getValue();
                logger.error(errorMessage);
                return ProcessResult.error(event, errorMessage);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý BalancesLockEvent: {}", e.getMessage(), e);
            return ProcessResult.error(event, "Lỗi khi xử lý BalancesLockEvent: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý tạo khóa số dư
     * 
     * @param lockEvent Event tạo khóa
     * @return ProcessResult chứa kết quả xử lý
     */
    private ProcessResult processCreateLock(BalancesLockEvent lockEvent) {
        // Log lockId từ event
        String requestedLockId = lockEvent.getLockId();
        logger.debug("Xử lý yêu cầu tạo khóa với ID: {}", requestedLockId != null ? requestedLockId : "(auto-generated)");
        
        // Tạo BalanceLock mới
        BalanceLock lock = lockEvent.toBalanceLock(false);
        
        // Map để lưu trữ các tài khoản đã xử lý
        Map<String, Account> processedAccounts = new HashMap<>();
        
        // Xử lý từng tài khoản trong danh sách
        for (String accountKey : lockEvent.getAccountKeys()) {
            // Lấy tài khoản từ cache
            Optional<Account> optAccount = accountCache.getAccount(accountKey);
            
            if (!optAccount.isPresent()) {
                logger.warn("Không tìm thấy tài khoản với key: {}", accountKey);
                continue;
            }
            
            Account account = optAccount.get();
            
            // Lấy số dư khả dụng
            BigDecimal availableBalance = account.getAvailableBalance();
            
            if (availableBalance.compareTo(BigDecimal.ZERO) > 0) {
                // Thêm số dư vào BalanceLock
                lock.addLockedBalance(accountKey, availableBalance);
                
                // Khóa số dư tài khoản
                freezeAccountBalance(account, availableBalance);
                
                // Lưu tài khoản đã xử lý
                processedAccounts.put(accountKey, account);
                
                logger.info("Đã khóa {} cho tài khoản {}", availableBalance, accountKey);
            } else {
                logger.info("Tài khoản {} không có số dư khả dụng để khóa", accountKey);
            }
        }
        
        // Kiểm tra nếu không có số dư nào được khóa
        if (lock.getLockedBalances().isEmpty()) {
            logger.info("Không có số dư nào được khóa trong danh sách tài khoản");
        }
        
        // Lưu BalanceLock vào cache
        balanceLockCache.addBalanceLock(lock);
        
        // Cập nhật kết quả
        if (!processedAccounts.isEmpty()) {
            // Chỉ lưu tài khoản đầu tiên vào kết quả cho khả năng tương thích
            Account firstAccount = processedAccounts.values().iterator().next();
            result.setAccount(firstAccount);
        }
        result.setBalanceLock(lock);
        
        // Kiểm tra và log nếu có sự thay đổi ID
        if (requestedLockId != null && !requestedLockId.isEmpty() && !requestedLockId.equals(lock.getLockId())) {
            logger.warn("Lưu ý: ID khóa đã thay đổi từ {} thành {}", requestedLockId, lock.getLockId());
        }
        
        logger.info("Đã tạo khóa {} cho {} tài khoản với tổng {} khoản tiền bị khóa", 
                lock.getLockId(), lockEvent.getAccountKeys().size(), lock.getLockedBalances().size());
        
        return result;
    }
    
    /**
     * Xử lý giải phóng khóa số dư
     * 
     * @param lockEvent Event giải phóng khóa
     * @return ProcessResult chứa kết quả xử lý
     */
    private ProcessResult processReleaseLock(BalancesLockEvent lockEvent) {
        String lockId = lockEvent.getLockId();
        
        // Lấy BalanceLock từ cache
        Optional<BalanceLock> optLock = balanceLockCache.getBalanceLock(lockId);
        
        if (!optLock.isPresent()) {
            String errorMessage = "Không tìm thấy khóa với ID: " + lockId;
            logger.error(errorMessage);
            return ProcessResult.error(event, errorMessage);
        }
        
        BalanceLock lock = optLock.get();
        
        // Kiểm tra xem khóa đã được giải phóng chưa
        if ("RELEASED".equals(lock.getStatus())) {
            String errorMessage = "Khóa đã được giải phóng trước đó: " + lockId;
            logger.warn(errorMessage);
            return ProcessResult.error(event, errorMessage);
        }
        
        // Map để lưu trữ các tài khoản đã xử lý
        Map<String, Account> processedAccounts = new HashMap<>();
        
        // Giải phóng số dư cho từng tài khoản
        for (Map.Entry<String, BigDecimal> entry : lock.getLockedBalances().entrySet()) {
            String accountKey = entry.getKey();
            BigDecimal lockedAmount = entry.getValue();
            
            // Lấy tài khoản
            Optional<Account> optAccount = accountCache.getAccount(accountKey);
            
            if (!optAccount.isPresent()) {
                logger.warn("Không tìm thấy tài khoản với key: {}", accountKey);
                continue;
            }
            
            Account account = optAccount.get();
            
            // Giải phóng số dư đã khóa
            unfreezeAccountBalance(account, lockedAmount);
            
            // Lưu tài khoản đã xử lý
            processedAccounts.put(accountKey, account);
            
            logger.info("Đã giải phóng {} cho tài khoản {}", lockedAmount, accountKey);
        }
        
        // Cập nhật trạng thái khóa
        lock.setStatus("RELEASED");
        
        // Lưu BalanceLock vào cache
        balanceLockCache.addBalanceLock(lock);
        
        // Cập nhật kết quả
        if (!processedAccounts.isEmpty()) {
            // Chỉ lưu tài khoản đầu tiên vào kết quả cho khả năng tương thích
            Account firstAccount = processedAccounts.values().iterator().next();
            result.setAccount(firstAccount);
        }
        result.setBalanceLock(lock);
        
        logger.info("Đã giải phóng khóa {} cho {} tài khoản với tổng {} khoản tiền được trả lại", 
                lockId, lock.getAccountKeys().size(), lock.getLockedBalances().size());
        
        return result;
    }
    
    /**
     * Khóa số dư tài khoản
     * 
     * @param account Tài khoản cần khóa
     * @param amountToFreeze Số lượng cần khóa
     */
    private void freezeAccountBalance(Account account, BigDecimal amountToFreeze) {
        if (amountToFreeze.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        // Lấy số dư hiện tại
        BigDecimal currentAvailable = account.getAvailableBalance();
        BigDecimal currentFrozen = account.getFrozenBalance();
        
        // Giảm số dư khả dụng và tăng số dư bị khóa
        account.setAvailableBalance(currentAvailable.subtract(amountToFreeze));
        account.setFrozenBalance(currentFrozen.add(amountToFreeze));
        
        // Lưu tài khoản vào cache
        accountCache.addAccountToBatch(account);
    }
    
    /**
     * Giải phóng số dư đã khóa
     * 
     * @param account Tài khoản cần giải phóng
     * @param amountToUnfreeze Số lượng cần giải phóng
     */
    private void unfreezeAccountBalance(Account account, BigDecimal amountToUnfreeze) {
        if (amountToUnfreeze.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        // Lấy số dư hiện tại
        BigDecimal currentAvailable = account.getAvailableBalance();
        BigDecimal currentFrozen = account.getFrozenBalance();
        
        // Tăng số dư khả dụng và giảm số dư bị khóa
        account.setAvailableBalance(currentAvailable.add(amountToUnfreeze));
        account.setFrozenBalance(currentFrozen.subtract(amountToUnfreeze));
        
        // Lưu tài khoản vào cache
        accountCache.addAccountToBatch(account);
    }
} 
