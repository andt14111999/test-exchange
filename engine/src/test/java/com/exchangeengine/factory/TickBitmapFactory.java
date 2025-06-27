package com.exchangeengine.factory;

import java.util.BitSet;
import java.util.Random;

import org.instancio.Instancio;
import org.instancio.Model;

import com.exchangeengine.model.TickBitmap;

import static org.instancio.Select.field;

/**
 * Factory class for creating TickBitmap objects for testing purposes.
 */
public class TickBitmapFactory {

  private static final Random random = new Random();

  /**
   * Creates a model for a TickBitmap with basic parameters.
   *
   * @param poolPair the pool pair (e.g., "BTC-USDT")
   * @return a model of TickBitmap
   */
  private Model<TickBitmap> model(String poolPair) {
    return Instancio.of(TickBitmap.class)
        .set(field("poolPair"), poolPair)
        .set(field("bitmap"), new BitSet())
        .set(field("createdAt"), System.currentTimeMillis())
        .set(field("updatedAt"), System.currentTimeMillis())
        .toModel();
  }

  /**
   * Creates a default empty TickBitmap for a given pool pair.
   *
   * @param poolPair the pool pair (e.g., "BTC-USDT")
   * @return a new empty TickBitmap
   */
  public TickBitmap createEmptyBitmap(String poolPair) {
    return Instancio.create(model(poolPair));
  }

  /**
   * Creates a TickBitmap with specific bits set.
   *
   * @param poolPair  the pool pair
   * @param positions array of bit positions to set to 1
   * @return a TickBitmap with specified bits set
   */
  public TickBitmap createBitmapWithBits(String poolPair, int[] positions) {
    TickBitmap bitmap = createEmptyBitmap(poolPair);

    for (int position : positions) {
      bitmap.setBit(position);
    }

    return bitmap;
  }

  /**
   * Creates a TickBitmap with a range of bits set.
   *
   * @param poolPair the pool pair
   * @param start    start position (inclusive)
   * @param end      end position (exclusive)
   * @return a TickBitmap with a range of bits set
   */
  public TickBitmap createBitmapWithRange(String poolPair, int start, int end) {
    TickBitmap bitmap = createEmptyBitmap(poolPair);

    for (int i = start; i < end; i++) {
      bitmap.setBit(i);
    }

    return bitmap;
  }

  /**
   * Creates a random TickBitmap with random bits set.
   *
   * @param poolPair the pool pair
   * @param numBits  approximate number of bits to set
   * @return a randomly populated TickBitmap
   */
  public TickBitmap createRandomBitmap(String poolPair, int numBits) {
    TickBitmap bitmap = createEmptyBitmap(poolPair);

    // Set random bits
    for (int i = 0; i < numBits; i++) {
      // Random position between -100000 and 100000
      int position = random.nextInt(200000) - 100000;
      bitmap.setBit(position);
    }

    return bitmap;
  }

  /**
   * Creates a TickBitmap from an existing BitSet.
   *
   * @param poolPair the pool pair
   * @param bitSet   existing BitSet to use
   * @return a TickBitmap based on the provided BitSet
   */
  public TickBitmap createFromBitSet(String poolPair, BitSet bitSet) {
    return Instancio.of(model(poolPair))
        .set(field("bitmap"), bitSet)
        .create();
  }

  /**
   * Creates a TickBitmap by deserializing from a byte array.
   *
   * @param poolPair  the pool pair
   * @param byteArray the byte array representation
   * @return a TickBitmap deserialized from the byte array
   */
  public TickBitmap createFromByteArray(String poolPair, byte[] byteArray) {
    TickBitmap bitmap = createEmptyBitmap(poolPair);
    bitmap.fromByteArray(byteArray);
    return bitmap;
  }
}
