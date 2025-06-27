# Test Factories for Exchange Engine Project

## Overview

The project uses a pattern of factory classes to generate test objects in a clean, maintainable way. These factories help in creating model instances with default or custom values for testing purposes.

## Available Factories

- `AccountFactory` - Creates `Account` model instances
- `AccountHistoryFactory` - Creates `AccountHistory` model instances
- `CoinDepositFactory` - Creates `CoinDeposit` model instances
- `DisruptorEventFactory` - Creates `DisruptorEvent` model instances

## Why Factory Pattern?

1. **Separation of Concerns**: Each factory is responsible for creating instances of a specific model class
2. **Maintainability**: When model classes change, we only need to update the corresponding factory
3. **Reusability**: Factory methods can be reused across test classes
4. **Readability**: Tests become more readable with descriptive factory method names
5. **Flexibility**: Easy to create variations of objects with different attributes

## Usage Examples

### Basic Usage

Create model instances with default values:

```java
// Create an Account
Account account = AccountFactory.create("btc:user123");

// Create a CoinDeposit
CoinDeposit deposit = CoinDepositFactory.create("btc:user123", "txn123", new BigDecimal("2.5"));

// Create a DisruptorEvent
DisruptorEvent event = DisruptorEventFactory.createDepositEvent("btc:user123", "txn123", new BigDecimal("2.5"));

// Create an AccountHistory
AccountHistory history = AccountHistoryFactory.createForDeposit("btc:user123", "txn123");
```

### Creating Specialized Instances

Each factory provides specialized methods for creating objects with specific configurations:

```java
// Create an Account with custom balances
Account customAccount = AccountFactory.createWithBalances(
    "btc:user123", new BigDecimal("100.0"), new BigDecimal("25.0"));

// Create a CoinDeposit with custom status
CoinDeposit confirmedDeposit = CoinDepositFactory.createWithStatus(
    "btc:user123", "txn123", new BigDecimal("2.5"), "confirmed");

// Create an AccountHistory with balance changes
AccountHistory depositHistory = AccountHistoryFactory.createForDepositWithBalanceChanges(
    "btc:user123", "txn123",
    new BigDecimal("100.0"), new BigDecimal("102.5"),
    new BigDecimal("25.0"), new BigDecimal("25.0"));
```

### Advanced Example - Testing a Deposit Flow

```java
@Test
void testDepositFlow() {
    // Setup - Create initial objects
    Account initialAccount = AccountFactory.create("btc:user123");
    BigDecimal initialAvailable = initialAccount.getAvailableBalance();
    BigDecimal depositAmount = new BigDecimal("2.5");

    DisruptorEvent event = DisruptorEventFactory.createDepositEvent(
        "btc:user123", "txn123", depositAmount);

    // Action - Process the deposit using your service
    // ... service logic here ...

    // Create expected result
    BigDecimal expectedAvailable = initialAvailable.add(depositAmount);
    Account expectedAccount = AccountFactory.createWithBalances(
        "btc:user123", expectedAvailable, initialAccount.getFrozenBalance());

    // Assert - Verify the results
    assertEquals(expectedAvailable, updatedAccount.getAvailableBalance());
}
```

## Extending the Factories

To add a new factory method to an existing factory class:

1. Add a new public static method with a descriptive name
2. Implement the method to create and return the desired object
3. Document the method with JavaDoc

Example:

```java
/**
 * Creates a CoinDeposit with a specific transaction hash
 *
 * @param accountKey The account key
 * @param identifier Unique identifier for the deposit
 * @param amount The deposit amount
 * @param txHash The transaction hash
 * @return A CoinDeposit instance with the specified transaction hash
 */
public static CoinDeposit createWithTxHash(String accountKey, String identifier,
                                         BigDecimal amount, String txHash) {
    CoinDeposit deposit = create(accountKey, identifier, amount);
    deposit.setTxHash(txHash);
    return deposit;
}
```

## Best Practices

1. Always use factories to create test objects instead of direct instantiation
2. Keep default values reasonable and representative of real data
3. Add specialized factory methods when you need variations of an object
4. Use descriptive method names that indicate what kind of object is being created
5. Document factory methods with JavaDoc
