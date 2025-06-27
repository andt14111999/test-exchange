package com.exchangeengine.factory;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.BalanceLock;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class for creating BalanceLock instances for tests
 */
public class BalanceLockFactory {

    private static final int DEFAULT_SCALE = 16;

    /**
     * Creates a model for BalanceLock with default values
     *
     * @return Model<BalanceLock> that can be used to create BalanceLock instances
     */
    public static Model<BalanceLock> model() {
        List<String> defaultAccountKeys = Arrays.asList("account1", "account2");
        Map<String, BigDecimal> defaultLockedBalances = new HashMap<>();
        defaultLockedBalances.put("account1", new BigDecimal("100.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP));
        defaultLockedBalances.put("account2", new BigDecimal("200.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP));

        return Instancio.of(BalanceLock.class)
                .set(field(BalanceLock::getLockId), UUID.randomUUID().toString())
                .set(field(BalanceLock::getAccountKeys), defaultAccountKeys)
                .set(field(BalanceLock::getIdentifier), "test-identifier")
                .set(field(BalanceLock::getActionType), ActionType.TRADE)
                .set(field(BalanceLock::getActionId), "test-action-id")
                .set(field(BalanceLock::getLockedBalances), defaultLockedBalances)
                .set(field(BalanceLock::getCreatedAt), Instant.now().toEpochMilli())
                .set(field(BalanceLock::getStatus), "LOCKED")
                .toModel();
    }

    /**
     * Creates a BalanceLock with default values
     *
     * @return A BalanceLock instance
     */
    public static BalanceLock create() {
        return Instancio.create(model());
    }

    /**
     * Creates a BalanceLock with specific parameters
     *
     * @param actionType   The action type
     * @param actionId     The action ID
     * @param accountKeys  The account keys
     * @param identifier   The identifier
     * @return A BalanceLock instance
     */
    public static BalanceLock create(ActionType actionType, String actionId, List<String> accountKeys, String identifier) {
        return new BalanceLock(actionType, actionId, accountKeys, identifier);
    }

    /**
     * Creates a BalanceLock with custom locked balances
     *
     * @param actionType      The action type
     * @param actionId        The action ID
     * @param accountKeys     The account keys
     * @param identifier      The identifier
     * @param lockedBalances  The locked balances map
     * @return A BalanceLock instance
     */
    public static BalanceLock createWithBalances(ActionType actionType, String actionId, List<String> accountKeys, 
                                                String identifier, Map<String, BigDecimal> lockedBalances) {
        BalanceLock balanceLock = new BalanceLock(actionType, actionId, accountKeys, identifier);
        if (lockedBalances != null) {
            balanceLock.setLockedBalances(lockedBalances);
        }
        return balanceLock;
    }

    /**
     * Creates a BalanceLock with RELEASED status
     *
     * @return A BalanceLock instance with RELEASED status
     */
    public static BalanceLock createReleased() {
        BalanceLock balanceLock = create();
        balanceLock.setStatus("RELEASED");
        return balanceLock;
    }

    /**
     * Creates a BalanceLock with empty account keys (for validation testing)
     *
     * @return A BalanceLock instance with empty account keys
     */
    public static BalanceLock createWithEmptyAccountKeys() {
        return Instancio.of(model())
                .set(field(BalanceLock::getAccountKeys), Arrays.asList())
                .create();
    }

    /**
     * Creates a BalanceLock with null fields (for validation testing)
     *
     * @return A BalanceLock instance with null fields
     */
    public static BalanceLock createWithNullFields() {
        return new BalanceLock();
    }
} 