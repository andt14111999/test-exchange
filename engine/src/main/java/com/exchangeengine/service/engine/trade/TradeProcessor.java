package com.exchangeengine.service.engine.trade;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.Trade;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.TradeEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.model.OperationType;

/**
 * Processor for trade events.
 * Handles trade execution, matching offers, and updating account balances.
 */
public class TradeProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TradeProcessor.class);
    
    private final DisruptorEvent event;
    private final StorageService storageService;
    private final ProcessResult result;
    private final AccountCache accountCache;

    /**
     * Constructor with DisruptorEvent.
     *
     * @param event Event to process
     */
    public TradeProcessor(DisruptorEvent event) {
        this.event = event;
        this.storageService = StorageService.getInstance();
        this.accountCache = AccountCache.getInstance();
        this.result = new ProcessResult(event);
    }

    /**
     * Process trade event
     *
     * @return ProcessResult containing processing result
     */
    public ProcessResult process() {
        TradeEvent tradeEvent = event.getTradeEvent();
        
        if (tradeEvent == null) {
            event.setErrorMessage("Trade event is null");
            logger.error("TradeEvent is null");
            return result;
        }

        try {
            OperationType operationType = tradeEvent.getOperationType();
            
            switch (operationType) {
                case TRADE_CREATE:
                    processCreateOperation(tradeEvent);
                    break;
                case TRADE_COMPLETE:
                    processCompleteOperation(tradeEvent);
                    break;
                case TRADE_CANCEL:
                    processCancelOperation(tradeEvent);
                    break;
                default:
                    event.setErrorMessage("Unsupported operation type: " + operationType);
                    logger.error("Unsupported trade operation type: {}", operationType);
                    break;
            }
            
            if (event.isSuccess()) {
                // Check if we need to flush accounts to disk
                if (accountCache.accountCacheShouldFlush()) {
                    accountCache.flushAccountToDisk();
                }
                
                logger.info("Successfully processed trade operation: {}", operationType);
            }
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing trade event: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Process trade creation:
     * 1. Creates a new trade
     * 2. If takerSide is BUY, locks the fiat amount for seller
     *
     * @param tradeEvent The trade event
     */
    private void processCreateOperation(TradeEvent tradeEvent) {
        try {
            // Extract event data
            String identifier = tradeEvent.getIdentifier();
            String takerSide = tradeEvent.getTakerSide();
            BigDecimal coinAmount = tradeEvent.getCoinAmount();
            BigDecimal price = tradeEvent.getPrice();
            BigDecimal amountAfterFee = tradeEvent.getAmountAfterFee();
            
            // Validate inputs
            if (identifier == null || identifier.trim().isEmpty()) {
                event.setErrorMessage("Trade identifier is required");
                logger.error("Trade identifier is required");
                return;
            }
            
            if (coinAmount == null || coinAmount.compareTo(BigDecimal.ZERO) <= 0) {
                event.setErrorMessage("Coin amount must be greater than zero");
                logger.error("Coin amount must be greater than zero: {}", identifier);
                return;
            }
            
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                event.setErrorMessage("Price must be greater than zero");
                logger.error("Price must be greater than zero: {}", identifier);
                return;
            }
            
            if (takerSide == null || (!Trade.TAKER_SIDE_BUY.equalsIgnoreCase(takerSide) && 
                !Trade.TAKER_SIDE_SELL.equalsIgnoreCase(takerSide))) {
                event.setErrorMessage("Taker side must be BUY or SELL");
                logger.error("Taker side must be BUY or SELL: {}", identifier);
                return;
            }
            
            // Check if trade already exists
            if (tradeEvent.fetchTrade(false).isPresent()) {
                logger.info("Trade already exists, skipping creation: {}", identifier);
                event.successes();
                return;
            }

            // Create new trade
            Trade trade = tradeEvent.toTrade(false);
            
            // Get offer
            Optional<Offer> offerOpt = tradeEvent.getOffer();
            
            if (!offerOpt.isPresent()) {
                event.setErrorMessage("Offer not found");
                logger.error("Offer not found for trade creation: {}", identifier);
                return;
            }
            
            Offer offer = offerOpt.get();
            
            // Validate offer
            if (!offer.isActive()) {
                event.setErrorMessage("Offer is not active");
                logger.error("Offer is not active for trade creation: {}", identifier);
                return;
            }
            
            if (!offer.canBeFilled(coinAmount)) {
                event.setErrorMessage("Insufficient quantity in offer");
                logger.error("Insufficient quantity in offer for trade creation: {}", identifier);
                return;
            }
            
            // Validate offer type against taker side
            if ((Trade.TAKER_SIDE_BUY.equalsIgnoreCase(takerSide) && offer.getType() != Offer.OfferType.SELL) ||
                (Trade.TAKER_SIDE_SELL.equalsIgnoreCase(takerSide) && offer.getType() != Offer.OfferType.BUY)) {
                event.setErrorMessage("Offer type does not match taker side");
                logger.error("Offer type does not match taker side: {}, offer type: {}", takerSide, offer.getType());
                return;
            }
            
            // Reserve offer quantity
            offer.partiallyFill(coinAmount);
            
            // Update offer in cache
            tradeEvent.updateOffer(offer);
            
            // If takerSide is BUY, lock fiat amount for seller
            if (Trade.TAKER_SIDE_BUY.equalsIgnoreCase(takerSide)) {
                // Get seller account
                Optional<Account> sellerAccountOpt = tradeEvent.getSellerAccount();
                
                if (!sellerAccountOpt.isPresent()) {
                    event.setErrorMessage("Seller account not found");
                    logger.error("Seller account not found for trade creation: {}", identifier);
                    return;
                }
                
                Account sellerAccount = sellerAccountOpt.get();
                
                // Calculate fiat amount - khi taker_side là BUY (user chuyển tiền cho merchant)
                // Chỉ cần lock đủ amount_after_fee thôi
                BigDecimal fiatAmountToLock;
                
                // Sử dụng amountAfterFee nếu có, nếu không thì tính theo cách cũ
                if (amountAfterFee != null && amountAfterFee.compareTo(BigDecimal.ZERO) > 0) {
                    fiatAmountToLock = amountAfterFee;
                    logger.info("Using amountAfterFee for seller lock: {}", fiatAmountToLock);
                } else {
                    // Fallback nếu không có amountAfterFee
                    fiatAmountToLock = coinAmount.multiply(price);
                    logger.info("Using calculated fiatAmount for seller lock (no amountAfterFee): {}", fiatAmountToLock);
                }
                
                // Check if seller has sufficient fiat balance
                if (sellerAccount.getAvailableBalance().compareTo(fiatAmountToLock) < 0) {
                    event.setErrorMessage("Seller has insufficient fiat balance");
                    logger.error("Seller has insufficient fiat balance for trade creation: {}, required: {}", 
                        identifier, fiatAmountToLock);
                    // Revert offer quantity reservation
                    offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                    tradeEvent.updateOffer(offer);
                    return;
                }
                
                // Lock fiat amount for seller
                BigDecimal prevAvailable = sellerAccount.getAvailableBalance();
                BigDecimal prevFrozen = sellerAccount.getFrozenBalance();
                
                try {
                    sellerAccount.decreaseAvailableBalance(fiatAmountToLock);
                    sellerAccount.increaseFrozenBalance(fiatAmountToLock);
                    
                    // Create account history
                    AccountHistory sellerHistory = createAccountHistory(
                        sellerAccount, "trade_create", "Trade creation (seller fiat lock): " + identifier,
                        prevAvailable, prevFrozen);
                    
                    // Update seller account in cache
                    tradeEvent.updateSellerAccount(sellerAccount);
                    
                    // Set result
                    result.setSellerAccount(sellerAccount);
                    result.setSellerAccountHistory(sellerHistory);
                } catch (Exception e) {
                    logger.error("Error updating seller balance: {}", e.getMessage(), e);
                    event.setErrorMessage("Error updating seller balance: " + e.getMessage());
                    
                    // Revert offer quantity reservation
                    offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                    tradeEvent.updateOffer(offer);
                    return;
                }
            } else {
                // If takerSide is SELL, lock fiat amount for seller (taker)
                
                // Get seller account
                Optional<Account> sellerAccountOpt = tradeEvent.getSellerAccount();
                
                if (!sellerAccountOpt.isPresent()) {
                    event.setErrorMessage("Seller account not found");
                    logger.error("Seller account not found for trade creation: {}", identifier);
                    // Revert offer quantity reservation
                    offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                    tradeEvent.updateOffer(offer);
                    return;
                }
                
                Account sellerAccount = sellerAccountOpt.get();
                
                // Calculate fiat amount - khi taker_side là SELL (merchant chuyển tiền cho user)
                // Lock toàn bộ số tiền fiatAmount
                BigDecimal fiatAmount = coinAmount.multiply(price);
                
                // Check if seller has sufficient fiat balance
                if (sellerAccount.getAvailableBalance().compareTo(fiatAmount) < 0) {
                    event.setErrorMessage("Seller has insufficient fiat balance");
                    logger.error("Seller has insufficient fiat balance for trade creation: {}", identifier);
                    // Revert offer quantity reservation
                    offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                    tradeEvent.updateOffer(offer);
                    return;
                }
                
                // Lock fiat amount for seller
                BigDecimal sellerPrevAvailable = sellerAccount.getAvailableBalance();
                BigDecimal sellerPrevFrozen = sellerAccount.getFrozenBalance();
                
                try {
                    // Lock seller's fiat
                    sellerAccount.decreaseAvailableBalance(fiatAmount);
                    sellerAccount.increaseFrozenBalance(fiatAmount);
                    
                    // Create account history for seller
                    AccountHistory sellerHistory = createAccountHistory(
                        sellerAccount, "trade_create", "Trade creation (seller fiat lock): " + identifier,
                        sellerPrevAvailable, sellerPrevFrozen);
                    
                    // Update account in cache
                    tradeEvent.updateSellerAccount(sellerAccount);
                    
                    // Set result
                    result.setSellerAccount(sellerAccount);
                    result.setSellerAccountHistory(sellerHistory);
                } catch (Exception e) {
                    logger.error("Error updating account balances: {}", e.getMessage(), e);
                    event.setErrorMessage("Error updating account balances: " + e.getMessage());
                    
                    // Revert offer quantity reservation
                    offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                    tradeEvent.updateOffer(offer);
                    return;
                }
            }
            
            // Save trade to cache
            tradeEvent.updateTrade(trade);
            
            // Set results
            result.setOffer(offer);
            result.setTrade(trade);
            
            // If buyer account exists, set it in the result
            tradeEvent.getBuyerAccount().ifPresent(buyerAccount -> {
                result.setBuyerAccount(buyerAccount);
            });
            
            // Set success
            event.successes();
            
            logger.info("Trade creation successful: identifier={}, takerSide={}, coinAmount={}, price={}",
                identifier, takerSide, coinAmount, price);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing trade creation: {}", e.getMessage(), e);
        }
    }

    /**
     * Process trade payment (buyer marks trade as paid)
     * This operation completes the trade directly
     *
     * @param tradeEvent The trade event
     */
    private void processCompleteOperation(TradeEvent tradeEvent) {
        try {
            // Extract event data
            String identifier = tradeEvent.getIdentifier();
            
            // Get trade
            Optional<Trade> tradeOpt = tradeEvent.fetchTrade(true);
            
            if (!tradeOpt.isPresent()) {
                event.setErrorMessage("Trade not found");
                logger.error("Trade not found: {}", identifier);
                return;
            }
            
            Trade trade = tradeOpt.get();
            
            // Validate trade status
            if (!trade.isUnpaid()) {
                event.setErrorMessage("Trade is not in UNPAID status");
                logger.error("Trade is not in UNPAID status: {}", identifier);
                return;
            }
            
            // Update trade status to COMPLETED
            trade.complete();
            
            // Process account transfers for completed trade
            processAccountTransferForCompletedTrade(tradeEvent, trade);
            
            // Update trade in cache
            tradeEvent.updateTrade(trade);
            
            // Set results
            result.setTrade(trade);
            
            // If buyer and seller accounts exist, set them in the result
            tradeEvent.getBuyerAccount().ifPresent(buyerAccount -> {
                result.setBuyerAccount(buyerAccount);
            });
            
            tradeEvent.getSellerAccount().ifPresent(sellerAccount -> {
                result.setSellerAccount(sellerAccount);
            });
            
            // Set success
            event.successes();
            
            logger.info("Trade payment and completion successful: identifier={}", identifier);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing trade payment: {}", e.getMessage(), e);
        }
    }

    /**
     * Process account transfers for a completed trade
     * 
     * @param tradeEvent The trade event
     * @param trade The trade that is completed
     */
    private void processAccountTransferForCompletedTrade(TradeEvent tradeEvent, Trade trade) {
        // Get buyer and seller accounts
        Optional<Account> buyerAccountOpt = tradeEvent.getBuyerAccount();
        Optional<Account> sellerAccountOpt = tradeEvent.getSellerAccount();
        
        if (!buyerAccountOpt.isPresent() || !sellerAccountOpt.isPresent()) {
            event.setErrorMessage("Buyer or seller account not found");
            logger.error("Buyer or seller account not found for trade completion: {}", trade.getIdentifier());
            return;
        }
        
        Account buyerAccount = buyerAccountOpt.get();
        Account sellerAccount = sellerAccountOpt.get();
        
        // Cần xác định chính xác số tiền đã khóa để giải phóng
        BigDecimal amountToRelease;
        BigDecimal amountToTransfer;
        
        if (trade.isBuyerTaker()) {
            // Buyer is taker (trade with a sell offer)
            // Với takerSide=BUY, chỉ khóa đủ amountAfterFee
            
            // Số tiền để giải phóng từ tài khoản người bán
            if (trade.getAmountAfterFee() != null && trade.getAmountAfterFee().compareTo(BigDecimal.ZERO) > 0) {
                amountToRelease = trade.getAmountAfterFee();
                logger.info("Using amountAfterFee for release: {}", amountToRelease);
            } else {
                amountToRelease = trade.getCoinAmount().multiply(trade.getPrice());
                logger.info("Using calculated fiatAmount for release: {}", amountToRelease);
            }
            
            // Số tiền để chuyển cho người mua (luôn là số tiền sau phí)
            if (trade.getAmountAfterFee() != null && trade.getAmountAfterFee().compareTo(BigDecimal.ZERO) > 0) {
                amountToTransfer = trade.getAmountAfterFee();
            } else {
                amountToTransfer = trade.getCoinAmount().multiply(trade.getPrice());
            }
            
            // Move locked fiat from seller to buyer
            BigDecimal sellerPrevAvailable = sellerAccount.getAvailableBalance();
            BigDecimal sellerPrevFrozen = sellerAccount.getFrozenBalance();
            
            // Đảm bảo không giảm nhiều hơn số đã khóa
            if (sellerAccount.getFrozenBalance().compareTo(amountToRelease) < 0) {
                logger.warn("Seller has less frozen balance ({}) than expected to release ({})", 
                    sellerAccount.getFrozenBalance(), amountToRelease);
                amountToRelease = sellerAccount.getFrozenBalance();
            }
            
            sellerAccount.decreaseFrozenBalance(amountToRelease);
            
            BigDecimal buyerPrevAvailable = buyerAccount.getAvailableBalance();
            BigDecimal buyerPrevFrozen = buyerAccount.getFrozenBalance();
            
            buyerAccount.increaseAvailableBalance(amountToTransfer);
            
            // Create account history records
            AccountHistory sellerHistory = createAccountHistory(
                sellerAccount, "trade_complete", "Trade completion (fiat transfer to buyer): " + trade.getIdentifier(),
                sellerPrevAvailable, sellerPrevFrozen);
                
            AccountHistory buyerHistory = createAccountHistory(
                buyerAccount, "trade_complete", "Trade completion (fiat received from seller): " + trade.getIdentifier(),
                buyerPrevAvailable, buyerPrevFrozen);
            
            // Set histories in result
            result.setSellerAccountHistory(sellerHistory);
            result.setBuyerAccountHistory(buyerHistory);
        } else {
            // Seller is taker (trade with a buy offer)
            // Với takerSide=SELL, khóa toàn bộ fiatAmount
            
            // Số tiền để giải phóng từ tài khoản người bán (toàn bộ fiatAmount)
            amountToRelease = trade.getCoinAmount().multiply(trade.getPrice());
            
            // Số tiền để chuyển cho người mua (cũng là toàn bộ fiatAmount)
            amountToTransfer = amountToRelease; // Khóa bao nhiêu thì chuyển đi bấy nhiêu
            
            BigDecimal buyerPrevAvailable = buyerAccount.getAvailableBalance();
            BigDecimal buyerPrevFrozen = buyerAccount.getFrozenBalance();
            
            // Buyer nhận fiat từ seller
            buyerAccount.increaseAvailableBalance(amountToTransfer);
            
            BigDecimal sellerPrevAvailable = sellerAccount.getAvailableBalance();
            BigDecimal sellerPrevFrozen = sellerAccount.getFrozenBalance();
            
            // Đảm bảo không giảm nhiều hơn số đã khóa
            if (sellerAccount.getFrozenBalance().compareTo(amountToRelease) < 0) {
                logger.warn("Seller has less frozen balance ({}) than expected to release ({})", 
                    sellerAccount.getFrozenBalance(), amountToRelease);
                amountToRelease = sellerAccount.getFrozenBalance();
                // Cập nhật lại số tiền chuyển (nếu frozen balance ít hơn dự kiến)
                amountToTransfer = amountToRelease;
            }
            
            // Giải phóng tiền fiat đã khóa của seller và chuyển cho buyer
            sellerAccount.decreaseFrozenBalance(amountToRelease);
            
            // Create account history records
            AccountHistory buyerHistory = createAccountHistory(
                buyerAccount, "trade_complete", "Trade completion (received fiat from seller): " + trade.getIdentifier(),
                buyerPrevAvailable, buyerPrevFrozen);
                
            AccountHistory sellerHistory = createAccountHistory(
                sellerAccount, "trade_complete", "Trade completion (released fiat to buyer): " + trade.getIdentifier(),
                sellerPrevAvailable, sellerPrevFrozen);
            
            // Set histories in result
            result.setBuyerAccountHistory(buyerHistory);
            result.setSellerAccountHistory(sellerHistory);
        }
        
        // Update accounts in cache
        tradeEvent.updateBuyerAccount(buyerAccount);
        tradeEvent.updateSellerAccount(sellerAccount);
        
        // Set account results
        result.setBuyerAccount(buyerAccount);
        result.setSellerAccount(sellerAccount);
    }

    /**
     * Process trade cancellation:
     * 1. Validates the trade
     * 2. Reverts account balances
     * 3. Reverts offer quantities
     *
     * @param tradeEvent The trade event
     */
    private void processCancelOperation(TradeEvent tradeEvent) {
        try {
            // Extract event data
            String identifier = tradeEvent.getIdentifier();
            
            // Validate inputs
            if (identifier == null || identifier.trim().isEmpty()) {
                event.setErrorMessage("Trade identifier is required");
                logger.error("Trade identifier is required");
                return;
            }
            
            // Get trade
            Optional<Trade> tradeOpt = tradeEvent.fetchTrade(true);
            
            if (!tradeOpt.isPresent()) {
                event.setErrorMessage("Trade not found");
                logger.error("Trade not found: {}", identifier);
                return;
            }
            
            Trade trade = tradeOpt.get();
            
            // Validate trade status
            if (trade.isCompleted() || trade.isCancelled()) {
                event.setErrorMessage("Trade cannot be cancelled in current status");
                logger.error("Trade cannot be cancelled in current status: {}", identifier);
                return;
            }
            
            // Get offer
            Optional<Offer> offerOpt = tradeEvent.getOffer();
            
            if (!offerOpt.isPresent()) {
                event.setErrorMessage("Offer not found");
                logger.error("Offer not found for trade cancellation: {}", identifier);
                return;
            }
            
            Offer offer = offerOpt.get();
            
            // Validate offer
            if (offer.getDeleted() != null && offer.getDeleted()) {
                event.setErrorMessage("Offer is deleted");
                logger.error("Offer is deleted for trade cancellation: {}", identifier);
                
                // Still cancel the trade but without reverting quantities
                trade.cancel();
                trade.updateStatus(Trade.TradeStatus.CANCELLED, "Offer was deleted, trade cancelled without reverting quantities");
                tradeEvent.updateTrade(trade);
                
                result.setTrade(trade);
                event.successes();
                logger.info("Trade cancelled without reverting quantities (offer deleted): {}", identifier);
                return;
            }
            
            // Revert offer quantity
            BigDecimal coinAmount = trade.getCoinAmount();
            boolean offerUpdated = false;
            
            try {
                offer.setAvailableAmount(offer.getAvailableAmount().add(coinAmount));
                
                // Update offer status if needed
                if (offer.getAvailableAmount().compareTo(offer.getTotalAmount()) == 0) {
                    offer.setStatus(Offer.OfferStatus.PENDING);
                } else {
                    offer.setStatus(Offer.OfferStatus.PARTIALLY_FILLED);
                }
                
                // Update offer in cache
                tradeEvent.updateOffer(offer);
                offerUpdated = true;
            } catch (Exception e) {
                logger.error("Error updating offer for trade cancellation: {}", e.getMessage(), e);
                event.setErrorMessage("Error updating offer: " + e.getMessage());
                return;
            }
            
            // If takerSide is BUY, unlock fiat for seller
            if (trade.isBuyerTaker() && trade.isUnpaid()) {
                // Get seller account
                Optional<Account> sellerAccountOpt = tradeEvent.getSellerAccount();
                
                if (!sellerAccountOpt.isPresent()) {
                    event.setErrorMessage("Seller account not found");
                    logger.error("Seller account not found for trade cancellation: {}", identifier);
                    
                    // Revert offer changes if the seller account is not found
                    if (offerUpdated) {
                        try {
                            offer.setAvailableAmount(offer.getAvailableAmount().subtract(coinAmount));
                            if (offer.getAvailableAmount().compareTo(offer.getTotalAmount()) < 0) {
                                offer.setStatus(Offer.OfferStatus.PARTIALLY_FILLED);
                            }
                            tradeEvent.updateOffer(offer);
                        } catch (Exception e) {
                            logger.error("Error reverting offer changes: {}", e.getMessage(), e);
                        }
                    }
                    return;
                }
                
                Account sellerAccount = sellerAccountOpt.get();
                
                // Calculate fiat amount
                BigDecimal fiatAmount = trade.getCoinAmount().multiply(trade.getPrice());
                
                // Unlock fiat amount for seller
                BigDecimal prevAvailable = sellerAccount.getAvailableBalance();
                BigDecimal prevFrozen = sellerAccount.getFrozenBalance();
                
                try {
                    if (sellerAccount.getFrozenBalance().compareTo(fiatAmount) < 0) {
                        logger.warn("Seller has insufficient frozen balance for cancellation. Expected: {}, Actual: {}", 
                            fiatAmount, sellerAccount.getFrozenBalance());
                        
                        // Use available frozen balance instead
                        BigDecimal availableFrozen = sellerAccount.getFrozenBalance();
                        sellerAccount.decreaseFrozenBalance(availableFrozen);
                        sellerAccount.increaseAvailableBalance(availableFrozen);
                        
                        AccountHistory sellerHistory = createAccountHistory(
                            sellerAccount, "trade_cancel", "Trade cancellation (partial seller fiat unlock): " + identifier,
                            prevAvailable, prevFrozen);
                        
                        tradeEvent.updateSellerAccount(sellerAccount);
                        result.setSellerAccount(sellerAccount);
                        result.setSellerAccountHistory(sellerHistory);
                        
                        logger.warn("Partial balance unlock performed for seller: {}", identifier);
                    } else {
                        // Normal case - frozen balance is sufficient
                        sellerAccount.decreaseFrozenBalance(fiatAmount);
                        sellerAccount.increaseAvailableBalance(fiatAmount);
                        
                        // Create account history
                        AccountHistory sellerHistory = createAccountHistory(
                            sellerAccount, "trade_cancel", "Trade cancellation (seller fiat unlock): " + identifier,
                            prevAvailable, prevFrozen);
                        
                        // Update seller account in cache
                        tradeEvent.updateSellerAccount(sellerAccount);
                        
                        // Set result
                        result.setSellerAccount(sellerAccount);
                        result.setSellerAccountHistory(sellerHistory);
                    }
                } catch (Exception e) {
                    logger.error("Error updating seller account for trade cancellation: {}", e.getMessage(), e);
                    event.setErrorMessage("Error updating seller account: " + e.getMessage());
                    
                    // Revert offer changes if the seller account update fails
                    if (offerUpdated) {
                        try {
                            offer.setAvailableAmount(offer.getAvailableAmount().subtract(coinAmount));
                            if (offer.getAvailableAmount().compareTo(offer.getTotalAmount()) < 0) {
                                offer.setStatus(Offer.OfferStatus.PARTIALLY_FILLED);
                            }
                            tradeEvent.updateOffer(offer);
                        } catch (Exception ex) {
                            logger.error("Error reverting offer changes: {}", ex.getMessage(), ex);
                        }
                    }
                    return;
                }
            } else if (!trade.isBuyerTaker() && trade.isUnpaid()) {
                // If seller is taker, unlock seller's frozen fiat
                
                // Get seller account
                Optional<Account> sellerAccountOpt = tradeEvent.getSellerAccount();
                
                if (!sellerAccountOpt.isPresent()) {
                    event.setErrorMessage("Seller account not found");
                    logger.error("Seller account not found for trade cancellation: {}", identifier);
                    
                    // Revert offer changes if the seller account is not found
                    if (offerUpdated) {
                        try {
                            offer.setAvailableAmount(offer.getAvailableAmount().subtract(coinAmount));
                            if (offer.getAvailableAmount().compareTo(offer.getTotalAmount()) < 0) {
                                offer.setStatus(Offer.OfferStatus.PARTIALLY_FILLED);
                            }
                            tradeEvent.updateOffer(offer);
                        } catch (Exception e) {
                            logger.error("Error reverting offer changes: {}", e.getMessage(), e);
                        }
                    }
                    return;
                }
                
                Account sellerAccount = sellerAccountOpt.get();
                
                // Calculate fiat amount
                BigDecimal fiatAmount = trade.getCoinAmount().multiply(trade.getPrice());
                
                // Unlock fiat for seller
                BigDecimal sellerPrevAvailable = sellerAccount.getAvailableBalance();
                BigDecimal sellerPrevFrozen = sellerAccount.getFrozenBalance();
                
                try {
                    // Handle seller's frozen fiat
                    if (sellerAccount.getFrozenBalance().compareTo(fiatAmount) < 0) {
                        logger.warn("Seller has insufficient frozen balance for cancellation. Expected: {}, Actual: {}", 
                            fiatAmount, sellerAccount.getFrozenBalance());
                        
                        // Use available frozen balance
                        BigDecimal availableFrozen = sellerAccount.getFrozenBalance();
                        sellerAccount.decreaseFrozenBalance(availableFrozen);
                        sellerAccount.increaseAvailableBalance(availableFrozen);
                        
                        logger.warn("Partial fiat unlock performed for seller: {}", identifier);
                    } else {
                        // Normal case - seller's frozen fiat is sufficient
                        sellerAccount.decreaseFrozenBalance(fiatAmount);
                        sellerAccount.increaseAvailableBalance(fiatAmount);
                    }
                    
                    // Create account history record
                    AccountHistory sellerHistory = createAccountHistory(
                        sellerAccount, "trade_cancel", "Trade cancellation (seller fiat unlock): " + identifier,
                        sellerPrevAvailable, sellerPrevFrozen);
                    
                    // Update account in cache
                    tradeEvent.updateSellerAccount(sellerAccount);
                    
                    // Set results
                    result.setSellerAccount(sellerAccount);
                    result.setSellerAccountHistory(sellerHistory);
                } catch (Exception e) {
                    logger.error("Error updating seller account for trade cancellation: {}", e.getMessage(), e);
                    event.setErrorMessage("Error updating seller account: " + e.getMessage());
                    
                    // Revert offer changes if the seller account update fails
                    if (offerUpdated) {
                        try {
                            offer.setAvailableAmount(offer.getAvailableAmount().subtract(coinAmount));
                            if (offer.getAvailableAmount().compareTo(offer.getTotalAmount()) < 0) {
                                offer.setStatus(Offer.OfferStatus.PARTIALLY_FILLED);
                            }
                            tradeEvent.updateOffer(offer);
                        } catch (Exception ex) {
                            logger.error("Error reverting offer changes: {}", ex.getMessage(), ex);
                        }
                    }
                    return;
                }
            }
            
            // Update trade status
            trade.cancel();
            
            // Update trade in cache
            tradeEvent.updateTrade(trade);
            
            // Set results
            result.setOffer(offer);
            result.setTrade(trade);
            
            // If buyer account exists, set it in the result
            tradeEvent.getBuyerAccount().ifPresent(buyerAccount -> {
                result.setBuyerAccount(buyerAccount);
            });
            
            // Set success
            event.successes();
            
            logger.info("Trade cancellation successful: identifier={}", identifier);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing trade cancellation: {}", e.getMessage(), e);
        }
    }

    /**
     * Create account history
     * 
     * @param account Account
     * @param action Action
     * @param message Message
     * @param prevAvailableBalance Previous available balance
     * @param prevFrozenBalance Previous frozen balance
     * @return Account history
     */
    private AccountHistory createAccountHistory(Account account, String action, String message,
                                              BigDecimal prevAvailableBalance, BigDecimal prevFrozenBalance) {
        // Create account history with account key, identifier, and operation type
        AccountHistory accountHistory = new AccountHistory(account.getKey(), message, action);
        
        // Set balance values
        accountHistory.setBalanceValues(
            prevAvailableBalance,
            account.getAvailableBalance(),
            prevFrozenBalance,
            account.getFrozenBalance()
        );
        
        // Store account history in cache
        try {
            if (storageService != null && storageService.getAccountHistoryCache() != null) {
                storageService.getAccountHistoryCache().addHistoryToBatch(accountHistory);
            } else {
                logger.warn("Cannot store account history: AccountHistoryCache is null");
            }
        } catch (Exception e) {
            logger.warn("Error storing account history: {}", e.getMessage());
        }
        
        return accountHistory;
    }
}
