package com.exchangeengine.factory;

import java.math.BigDecimal;
import java.util.Random;

import org.instancio.Instancio;
import org.instancio.Model;

import com.exchangeengine.model.Tick;

import static org.instancio.Select.field;

/**
 * Factory class for creating Tick objects for testing purposes.
 */
public class TickFactory {

  private static final Random random = new Random();

  /**
   * Creates a model for a Tick object with basic parameters.
   *
   * @param poolPair  the pool pair (e.g., "BTC-USDT")
   * @param tickIndex the tick index
   * @return a model of Tick
   */
  private Model<Tick> model(String poolPair, int tickIndex) {
    return Instancio.of(Tick.class)
        .set(field("poolPair"), poolPair)
        .set(field("tickIndex"), tickIndex)
        .set(field("liquidityGross"), BigDecimal.ZERO)
        .set(field("liquidityNet"), BigDecimal.ZERO)
        .set(field("feeGrowthOutside0"), BigDecimal.ZERO)
        .set(field("feeGrowthOutside1"), BigDecimal.ZERO)
        .set(field("tickInitializedTimestamp"), System.currentTimeMillis())
        .set(field("initialized"), false)
        .set(field("createdAt"), System.currentTimeMillis())
        .set(field("updatedAt"), System.currentTimeMillis())
        .toModel();
  }

  /**
   * Creates a default Tick object with given pool pair and tick index.
   *
   * @param poolPair  the pool pair (e.g., "BTC-USDT")
   * @param tickIndex the tick index
   * @return a new Tick object
   */
  public Tick createTick(String poolPair, int tickIndex) {
    return Instancio.create(model(poolPair, tickIndex));
  }

  /**
   * Creates an initialized Tick object with liquidity.
   *
   * @param poolPair       the pool pair (e.g., "BTC-USDT")
   * @param tickIndex      the tick index
   * @param liquidityGross the gross liquidity amount
   * @param liquidityNet   the net liquidity amount (can be negative)
   * @return an initialized Tick object with the specified liquidity
   */
  public Tick createInitializedTick(String poolPair, int tickIndex, BigDecimal liquidityGross,
      BigDecimal liquidityNet) {
    return Instancio.of(model(poolPair, tickIndex))
        .set(field("liquidityGross"), liquidityGross)
        .set(field("liquidityNet"), liquidityNet)
        .set(field("initialized"), true)
        .create();
  }

  /**
   * Creates a Tick object with all fee growth parameters set.
   *
   * @param poolPair          the pool pair
   * @param tickIndex         the tick index
   * @param feeGrowthOutside0 fee growth outside for token0
   * @param feeGrowthOutside1 fee growth outside for token1
   * @return a Tick object with fee growth parameters set
   */
  public Tick createTickWithFeeGrowth(String poolPair, int tickIndex, BigDecimal feeGrowthOutside0,
      BigDecimal feeGrowthOutside1) {
    return Instancio.of(model(poolPair, tickIndex))
        .set(field("feeGrowthOutside0"), feeGrowthOutside0)
        .set(field("feeGrowthOutside1"), feeGrowthOutside1)
        .set(field("initialized"), true)
        .create();
  }

  /**
   * Creates a fully configured Tick object with all parameters set.
   *
   * @param poolPair          the pool pair
   * @param tickIndex         the tick index
   * @param liquidityGross    the gross liquidity amount
   * @param liquidityNet      the net liquidity amount
   * @param feeGrowthOutside0 fee growth outside for token0
   * @param feeGrowthOutside1 fee growth outside for token1
   * @param initialized       whether the tick is initialized
   * @return a fully configured Tick object
   */
  public Tick createFullTick(String poolPair, int tickIndex, BigDecimal liquidityGross, BigDecimal liquidityNet,
      BigDecimal feeGrowthOutside0, BigDecimal feeGrowthOutside1, boolean initialized) {
    return Instancio.of(model(poolPair, tickIndex))
        .set(field("liquidityGross"), liquidityGross)
        .set(field("liquidityNet"), liquidityNet)
        .set(field("feeGrowthOutside0"), feeGrowthOutside0)
        .set(field("feeGrowthOutside1"), feeGrowthOutside1)
        .set(field("initialized"), initialized)
        .create();
  }

  /**
   * Creates a random Tick object for testing.
   *
   * @param poolPair the pool pair
   * @return a randomly configured Tick object
   */
  public Tick createRandomTick(String poolPair) {
    int tickIndex = random.nextInt(887272 * 2) - 887272; // Random tick between MIN_TICK and MAX_TICK

    // Generate random BigDecimal values
    BigDecimal liquidityGross = new BigDecimal(random.nextDouble() * 1000000);

    // For liquidityNet, we can have positive or negative values
    BigDecimal liquidityNet = new BigDecimal(random.nextDouble() * 1000000);
    if (random.nextBoolean()) {
      liquidityNet = liquidityNet.negate();
    }

    BigDecimal feeGrowthOutside0 = new BigDecimal(random.nextDouble() * 1000000);
    BigDecimal feeGrowthOutside1 = new BigDecimal(random.nextDouble() * 1000000);
    boolean initialized = random.nextBoolean();

    return createFullTick(poolPair, tickIndex, liquidityGross, liquidityNet, feeGrowthOutside0, feeGrowthOutside1,
        initialized);
  }
}
