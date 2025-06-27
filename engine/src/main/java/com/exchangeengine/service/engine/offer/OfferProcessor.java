package com.exchangeengine.service.engine.offer;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Offer;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.model.OperationType;

/**
 * Processor for offer events.
 * Handles create, update, disable, enable and delete operations for offers.
 */
public class OfferProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OfferProcessor.class);
    
    private final DisruptorEvent event;
    private final ProcessResult result;

    /**
     * Constructor with DisruptorEvent.
     *
     * @param event Event to process
     */
    public OfferProcessor(DisruptorEvent event) {
        this.event = event;
        this.result = new ProcessResult(event);
    }

    /**
     * Process offer event
     *
     * @return ProcessResult containing processing result
     */
    public ProcessResult process() {
        OfferEvent offerEvent = event.getOfferEvent();
        
        if (offerEvent == null) {
            event.setErrorMessage("Offer event is null");
            logger.error("OfferEvent is null");
            return result;
        }

        try {
            OperationType operationType = offerEvent.getOperationType();
            
            switch (operationType) {
                case OFFER_CREATE:
                    processCreateOperation(offerEvent);
                    break;
                case OFFER_UPDATE:
                    processUpdateOperation(offerEvent);
                    break;
                case OFFER_DISABLE:
                    processDisableOperation(offerEvent);
                    break;
                case OFFER_ENABLE:
                    processEnableOperation(offerEvent);
                    break;
                case OFFER_DELETE:
                    processDeleteOperation(offerEvent);
                    break;
                default:
                    event.setErrorMessage("Unsupported operation type: " + operationType);
                    logger.error("Unsupported offer operation type: {}", operationType);
                    break;
            }
            
            if (event.isSuccess()) {
                logger.info("Successfully processed offer operation: {}", operationType);
            }
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer event: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Process create operation:
     * 1. Creates offer record
     *
     * @param offerEvent The offer event
     */
    private void processCreateOperation(OfferEvent offerEvent) {
        // Extract event data
        String identifier = offerEvent.getIdentifier();
        String userId = offerEvent.getUserId();
        BigDecimal totalAmount = offerEvent.getTotalAmount();
        
        // Get or create offer
        Offer offer = offerEvent.toOffer(false);
        boolean isExistingOffer = offerEvent.fetchOffer(false).isPresent();
        
        if (isExistingOffer) {
            event.setErrorMessage("Offer already exists: " + identifier);
            logger.error("Offer already exists: {}", identifier);
            return;
        }
        
        try {
            // Update offer status
            offer.setDisabled(false);
            offer.setDeleted(false);
            offer.setAvailableAmount(totalAmount);
            
            // Save offer to cache immediately
            offerEvent.updateOffer(offer);
            
            // Set success result
            event.successes();
            
            // Set offer in result
            result.setOffer(offer);
            
            logger.info("Offer creation successful: identifier={}, totalAmount={}",
                identifier, totalAmount);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer creation: {}", e.getMessage(), e);
            
            // Create and mark offer as disabled
            Offer failedOffer = new Offer();
            failedOffer.setIdentifier(identifier);
            failedOffer.setUserId(userId);
            failedOffer.setType(Offer.OfferType.valueOf(offerEvent.getOfferType().toUpperCase()));
            failedOffer.setSymbol(offerEvent.getCoinCurrency() + ":" + offerEvent.getCurrency());
            failedOffer.setPrice(offerEvent.getPrice());
            failedOffer.setMinAmount(offerEvent.getMinAmount());
            failedOffer.setMaxAmount(offerEvent.getMaxAmount());
            failedOffer.setTotalAmount(totalAmount);
            failedOffer.setAvailableAmount(BigDecimal.ZERO);
            failedOffer.setPaymentMethodId(offerEvent.getPaymentMethodId());
            failedOffer.setPaymentTime(offerEvent.getPaymentTime());
            failedOffer.setCountryCode(offerEvent.getCountryCode());
            failedOffer.setDisabled(true);
            failedOffer.setDeleted(false);
            failedOffer.setAutomatic(offerEvent.getAutomatic());
            failedOffer.setOnline(offerEvent.getOnline());
            failedOffer.setMargin(offerEvent.getMargin());
            failedOffer.setStatus(Offer.OfferStatus.PENDING);
            
            result.setOffer(failedOffer);
        }
    }

    /**
     * Process update operation:
     * 1. Updates offer details
     *
     * @param offerEvent The offer event
     */
    private void processUpdateOperation(OfferEvent offerEvent) {
        // Extract event data
        String identifier = offerEvent.getIdentifier();
        BigDecimal totalAmount = offerEvent.getTotalAmount();
        BigDecimal availableAmount = offerEvent.getAvailableAmount();
        
        // Get offer
        Offer offer = offerEvent.toOffer(true);
        
        // Check if offer is not disabled or deleted
        if (offer.getDisabled() || offer.getDeleted()) {
            event.setErrorMessage("Cannot update: Offer is disabled or deleted: " + identifier);
            logger.error("Cannot update: Offer is disabled or deleted: {}", identifier);
            return;
        }
        
        try {
            // Update offer
            offer.setPrice(offerEvent.getPrice());
            offer.setMinAmount(offerEvent.getMinAmount());
            offer.setMaxAmount(offerEvent.getMaxAmount());
            offer.setTotalAmount(totalAmount);
            offer.setAvailableAmount(availableAmount);
            offer.setPaymentMethodId(offerEvent.getPaymentMethodId());
            offer.setPaymentTime(offerEvent.getPaymentTime());
            offer.setCountryCode(offerEvent.getCountryCode());
            offer.setAutomatic(offerEvent.getAutomatic());
            offer.setOnline(offerEvent.getOnline());
            offer.setMargin(offerEvent.getMargin());
            
            // Save offer to cache
            offerEvent.updateOffer(offer);
            
            // Set success result
            event.successes();
            
            // Set offer in result
            result.setOffer(offer);
            
            logger.info("Offer update successful: identifier={}, totalAmount={}, availableAmount={}",
                identifier, totalAmount, availableAmount);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer update: {}", e.getMessage(), e);
            
            // Keep the offer in its current state
            result.setOffer(offer);
        }
    }

    /**
     * Process disable operation:
     * 1. Disables the offer
     *
     * @param offerEvent The offer event
     */
    private void processDisableOperation(OfferEvent offerEvent) {
        // Extract event data
        String identifier = offerEvent.getIdentifier();
        
        // Get offer
        Offer offer = offerEvent.toOffer(true);
        
        // Check if offer is not already disabled or deleted
        if (offer.getDisabled() || offer.getDeleted()) {
            event.setErrorMessage("Offer is already disabled or deleted: " + identifier);
            logger.error("Offer is already disabled or deleted: {}", identifier);
            return;
        }
        
        try {
            // Step 1: Disable offer
            offer.setDisabled(true);
            
            // Save offer to cache
            offerEvent.updateOffer(offer);
            
            // Set success result
            event.successes();
            
            // Set offer in result
            result.setOffer(offer);
            
            logger.info("Offer disable successful: identifier={}", identifier);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer disable: {}", e.getMessage(), e);
            
            // Keep the offer in its current state
            result.setOffer(offer);
        }
    }

    /**
     * Process enable operation:
     * 1. Enables the offer
     *
     * @param offerEvent The offer event
     */
    private void processEnableOperation(OfferEvent offerEvent) {
        // Extract event data
        String identifier = offerEvent.getIdentifier();
        
        // Get offer
        Offer offer = offerEvent.toOffer(true);
        
        // Check if offer is not deleted
        if (offer.getDeleted()) {
            event.setErrorMessage("Cannot enable: Offer is deleted: " + identifier);
            logger.error("Cannot enable: Offer is deleted: {}", identifier);
            return;
        }
        
        try {
            // Step 1: Enable offer
            offer.setDisabled(false);
            
            // Save offer to cache
            offerEvent.updateOffer(offer);
            
            // Set success result
            event.successes();
            
            // Set offer in result
            result.setOffer(offer);
            
            logger.info("Offer enable successful: identifier={}", identifier);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer enable: {}", e.getMessage(), e);
            
            // Keep the offer in its current state
            result.setOffer(offer);
        }
    }

    /**
     * Process delete operation:
     * 1. Marks offer as deleted
     *
     * @param offerEvent The offer event
     */
    private void processDeleteOperation(OfferEvent offerEvent) {
        // Extract event data
        String identifier = offerEvent.getIdentifier();
        String offerType = offerEvent.getOfferType();
        BigDecimal totalAmount = offerEvent.getTotalAmount();
        
        // Get offer
        Offer offer = offerEvent.toOffer(true);
        
        // Check if offer is not already deleted
        if (offer.getDeleted()) {
            event.setErrorMessage("Offer is already deleted: " + identifier);
            logger.error("Offer is already deleted: {}", identifier);
            return;
        }
        
        try {
            // Delete offer
            offer.setDisabled(true);
            offer.setDeleted(true);
            offer.setAvailableAmount(BigDecimal.ZERO);
            
            // Save offer to cache
            offerEvent.updateOffer(offer);
            
            // Set success result
            event.successes();
            
            // Set offer in result
            result.setOffer(offer);
            
            logger.info("Offer deletion successful: identifier={}, offerType={}, totalAmount={}",
                identifier, offerType, totalAmount);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing offer deletion: {}", e.getMessage(), e);
            
            // Keep the offer in its current state
            result.setOffer(offer);
        }
    }
}
