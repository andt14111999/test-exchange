package com.exchangeengine.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Integration test để demonstrate fix cho floating point precision issue
 * từ production log
 */
public class FloatingPointPrecisionFixIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Test production withdrawal issue với amount 21.21 - should work perfectly after fix")
    public void testProductionWithdrawalIssue21_21() throws Exception {
        // Given - Chính xác như production log
        String productionMessageJson = "{"
            + "\"identifier\": \"75\","
            + "\"operationType\": \"coin_withdrawal_create\","
            + "\"actionType\": \"CoinTransaction\","
            + "\"actionId\": \"b91651aa-fd9e-4760-9655-86ce7f263ddd\","
            + "\"userId\": 12,"
            + "\"status\": \"verified\","
            + "\"accountKey\": \"12-coin-133\","
            + "\"amount\": \"21.21\","
            + "\"coin\": \"usdt\","
            + "\"txHash\": \"tx-ee9deb1aba88ab17278f8497bc584225\","
            + "\"layer\": \"L1\","
            + "\"destinationAddress\": \"address-b0b017d52f80177698002a45f83f058c\","
            + "\"fee\": \"0.0\","
            + "\"recipientAccountKey\": \"8-coin-85\","
            + "\"eventId\": \"950379b3-7bb0-4b85-9957-47724a0272dd\","
            + "\"timestamp\": 1751246435000"
            + "}";

        JsonNode messageJson = objectMapper.readTree(productionMessageJson);

        // When - Parse event từ JSON
        CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
        withdrawalEvent.parserData(messageJson);

        // Then - Verify amount parsing chính xác
        System.out.println("Production issue reproduction:");
        System.out.println("Original amount in JSON: 21.21");
        System.out.println("Parsed amount: " + withdrawalEvent.getAmount());
        System.out.println("Amount exact match: " + (new BigDecimal("21.21").equals(withdrawalEvent.getAmount())));

        // Amount phải chính xác 100%
        assertEquals(new BigDecimal("21.21"), withdrawalEvent.getAmount(), 
            "Amount should be exactly 21.21 - no floating point precision loss");
        
        // Verify fee cũng chính xác
        assertEquals(new BigDecimal("0.0"), withdrawalEvent.getFee(),
            "Fee should also be parsed exactly");

        System.out.println("✅ Production issue FIXED: Amount 21.21 parsed exactly!");
    }

    @Test
    @DisplayName("Test multiple amounts that caused production issues")
    public void testMultipleProblematicAmounts() throws Exception {
        // Given - Các amounts đã gây ra vấn đề trong production
        String[] problematicAmounts = {
            "21.21",    // From production log
            "10.50",    // Common decimal
            "99.99",    // Edge case
            "0.01",     // Minimum amount
            "1000.25"   // Larger amount
        };

        for (String amount : problematicAmounts) {
            // Create JSON message
            String messageJson = "{"
                + "\"identifier\": \"test-" + amount + "\","
                + "\"operationType\": \"coin_withdrawal_create\","
                + "\"actionType\": \"CoinTransaction\","
                + "\"actionId\": \"test-action-id\","
                + "\"status\": \"verified\","
                + "\"accountKey\": \"test-account\","
                + "\"amount\": \"" + amount + "\","
                + "\"coin\": \"usdt\","
                + "\"txHash\": \"tx-test\","
                + "\"layer\": \"L1\","
                + "\"destinationAddress\": \"test-address\","
                + "\"fee\": \"0.1\","
                + "\"eventId\": \"test-event-id\""
                + "}";

            JsonNode jsonNode = objectMapper.readTree(messageJson);

            // When - Parse event
            CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
            withdrawalEvent.parserData(jsonNode);

            // Then - Verify chính xác 100%
            assertEquals(new BigDecimal(amount), withdrawalEvent.getAmount(), 
                "Amount " + amount + " should be parsed exactly without precision loss");

            System.out.println("✓ Amount " + amount + " parsed correctly as: " + withdrawalEvent.getAmount());
        }
    }

    @Test
    @DisplayName("Test deposit events also work correctly with same fix")
    public void testDepositEventsPrecisionFix() throws Exception {
        // Given - Test deposit với cùng vấn đề
        String depositMessageJson = "{"
            + "\"identifier\": \"deposit-test\","
            + "\"operationType\": \"coin_deposit_create\","
            + "\"actionType\": \"CoinTransaction\","
            + "\"actionId\": \"deposit-action-id\","
            + "\"accountKey\": \"test-account\","
            + "\"amount\": \"21.21\","
            + "\"coin\": \"usdt\","
            + "\"txHash\": \"tx-deposit-test\","
            + "\"layer\": \"L1\","
            + "\"depositAddress\": \"deposit-address\","
            + "\"status\": \"pending\","
            + "\"statusExplanation\": \"Processing\","
            + "\"eventId\": \"deposit-event-id\""
            + "}";

        JsonNode messageJson = objectMapper.readTree(depositMessageJson);

        // When
        CoinDepositEvent depositEvent = new CoinDepositEvent();
        depositEvent.parserData(messageJson);

        // Then
        assertEquals(new BigDecimal("21.21"), depositEvent.getAmount(), 
            "Deposit amount should also be exactly 21.21 without precision loss");

        System.out.println("✓ Deposit amount parsed correctly as: " + depositEvent.getAmount());
    }

    @Test
    @DisplayName("Demonstrate the exact production bug that was fixed")
    public void testDemonstrateOriginalBugWasFixed() throws Exception {
        // Given - JSON với exact format từ production
        String jsonMessage = "{"
            + "\"amount\": \"21.21\","
            + "\"identifier\": \"test\","
            + "\"operationType\": \"coin_withdrawal_create\","
            + "\"actionType\": \"CoinTransaction\","
            + "\"actionId\": \"test\","
            + "\"accountKey\": \"test\","
            + "\"coin\": \"usdt\","
            + "\"txHash\": \"test\","
            + "\"layer\": \"L1\","
            + "\"destinationAddress\": \"test\","
            + "\"fee\": \"0.0\","
            + "\"status\": \"verified\","
            + "\"eventId\": \"test\""
            + "}";

        JsonNode jsonNode = objectMapper.readTree(jsonMessage);

        // When - Parse với fix mới
        CoinWithdrawalEvent event = new CoinWithdrawalEvent();
        event.parserData(jsonNode);

        // Then - Amount phải CHÍNH XÁC là 21.21, không có precision error
        String originalAmount = "21.21";
        BigDecimal parsedAmount = event.getAmount();
        
        System.out.println("=== FLOATING POINT PRECISION FIX VERIFICATION ===");
        System.out.println("Original JSON amount: " + originalAmount);
        System.out.println("Parsed amount: " + parsedAmount.toString());
        System.out.println("String representations match: " + originalAmount.equals(parsedAmount.toString()));
        System.out.println("BigDecimal equality: " + new BigDecimal(originalAmount).equals(parsedAmount));
        
        // Verification: Amount phải chính xác 100%
        assertEquals(new BigDecimal("21.21"), parsedAmount, 
            "After fix: Amount must be exactly 21.21 without any floating point precision loss");
        
        // Verification: String representation phải match
        assertEquals("21.21", parsedAmount.toString(),
            "String representation should match exactly");
            
        System.out.println("🎉 SUCCESS: Production floating point precision issue has been FIXED!");
    }
} 