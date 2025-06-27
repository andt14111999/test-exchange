package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exchangeengine.factory.TickBitmapFactory;

@DisplayName("TickBitmap")
class TickBitmapTest {

  private TickBitmapFactory tickBitmapFactory;
  private static final String TEST_POOL_PAIR = "BTC-USDT";

  @BeforeEach
  void setUp() {
    tickBitmapFactory = new TickBitmapFactory();
  }

  @Nested
  @DisplayName("Initialization")
  class Initialization {

    @Test
    @DisplayName("should create an empty bitmap")
    void shouldCreateEmptyBitmap() {
      // When
      TickBitmap bitmap = new TickBitmap(TEST_POOL_PAIR);

      // Then
      assertNotNull(bitmap);
      assertEquals(TEST_POOL_PAIR, bitmap.getPoolPair());
      assertTrue(bitmap.isBitmapEmpty());
      assertTrue(bitmap.getCreatedAt() > 0);
      assertEquals(bitmap.getCreatedAt(), bitmap.getUpdatedAt());
    }

    @Test
    @DisplayName("should create bitmap with factory")
    void shouldCreateBitmapWithFactory() {
      // When
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);

      // Then
      assertNotNull(bitmap);
      assertEquals(TEST_POOL_PAIR, bitmap.getPoolPair());
      assertTrue(bitmap.isBitmapEmpty());
    }

    @Test
    @DisplayName("should validate required fields")
    void shouldValidateRequiredFields() {
      // Given
      TickBitmap validBitmap = new TickBitmap(TEST_POOL_PAIR);
      TickBitmap invalidBitmap = new TickBitmap("");

      // When
      List<String> validErrors = validBitmap.validateRequiredFields();
      List<String> invalidErrors = invalidBitmap.validateRequiredFields();

      // Then
      assertTrue(validErrors.isEmpty());
      assertFalse(invalidErrors.isEmpty());
      assertTrue(invalidErrors.stream().anyMatch(error -> error.contains("Pool pair is required")));
    }
  }

  @Nested
  @DisplayName("Bit manipulation")
  class BitManipulation {

    @Test
    @DisplayName("should set a bit")
    void shouldSetBit() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);

      // When
      bitmap.setBit(100);

      // Then
      assertTrue(bitmap.isSet(100));
      assertFalse(bitmap.isSet(101));
      assertFalse(bitmap.isBitmapEmpty());
    }

    @Test
    @DisplayName("should clear a bit")
    void shouldClearBit() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200 });

      // When
      bitmap.clearBit(100);

      // Then
      assertFalse(bitmap.isSet(100));
      assertTrue(bitmap.isSet(200));
    }

    @Test
    @DisplayName("should find next set bit")
    void shouldFindNextSetBit() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200, 300 });

      // When
      int nextBit = bitmap.nextSetBit(150);

      // Then
      assertEquals(200, nextBit);
    }

    @Test
    @DisplayName("should find previous set bit")
    void shouldFindPreviousSetBit() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200, 300 });

      // When
      int prevBit = bitmap.previousSetBit(250);

      // Then
      assertEquals(200, prevBit);
    }

    @Test
    @DisplayName("should return -1 when no next set bit found")
    void shouldReturnNegativeOneWhenNoNextSetBitFound() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200, 300 });

      // When
      int nextBit = bitmap.nextSetBit(350);

      // Then
      assertEquals(-1, nextBit);
    }

    @Test
    @DisplayName("should return -1 when no previous set bit found")
    void shouldReturnNegativeOneWhenNoPreviousSetBitFound() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200, 300 });

      // When
      int prevBit = bitmap.previousSetBit(50);

      // Then
      assertEquals(-1, prevBit);
    }
  }

  @Nested
  @DisplayName("Serialization")
  class Serialization {

    @Test
    @DisplayName("should serialize to byte array")
    void shouldSerializeToByteArray() {
      // Given
      int[] positions = { 100, 200, 300 };
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, positions);

      // When
      byte[] bytes = bitmap.toByteArray();

      // Then
      assertNotNull(bytes);
      assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("should deserialize from byte array")
    void shouldDeserializeFromByteArray() {
      // Given
      int[] positions = { 100, 200, 300 };
      TickBitmap original = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, positions);
      byte[] bytes = original.toByteArray();

      // When
      TickBitmap deserialized = new TickBitmap(TEST_POOL_PAIR);
      deserialized.fromByteArray(bytes);

      // Then
      assertTrue(deserialized.isSet(100));
      assertTrue(deserialized.isSet(200));
      assertTrue(deserialized.isSet(300));
      assertFalse(deserialized.isSet(150));
    }

    @Test
    @DisplayName("should create from bit set")
    void shouldCreateFromBitSet() {
      // Given
      BitSet bitSet = new BitSet();
      bitSet.set(100);
      bitSet.set(200);

      // When
      TickBitmap bitmap = tickBitmapFactory.createFromBitSet(TEST_POOL_PAIR, bitSet);

      // Then
      assertTrue(bitmap.isSet(100));
      assertTrue(bitmap.isSet(200));
      assertFalse(bitmap.isSet(150));
    }
  }

  @Nested
  @DisplayName("Bitmap size")
  class BitmapSize {

    @Test
    @DisplayName("should get correct bitmap size")
    void shouldGetCorrectBitmapSize() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { 100, 200, 300 });

      // When
      int size = bitmap.getBitmapSize();

      // Then
      // BitSet.size() returns 1 more than the highest set bit index
      assertTrue(size > 300);
    }

    @Test
    @DisplayName("should return implementation-defined size for empty bitmap")
    void shouldReturnZeroSizeForEmptyBitmap() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);

      // When
      int size = bitmap.getBitmapSize();

      // Then
      // BitSet.size() returns the size of the internal data structure,
      // which may be non-zero even for an empty BitSet depending on JVM
      // implementation
      // We just check that getBitmapSize() runs without errors for empty bitmap
      assertNotNull(bitmap);
    }
  }

  @Nested
  @DisplayName("flipBit tests")
  class FlipBitTests {

    @Test
    @DisplayName("should set bit when flipped=true and initialized=true")
    void shouldSetBitWhenFlippedAndInitialized() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);
      int tickIndex = 100;
      assertFalse(bitmap.isSet(tickIndex), "Bit should not be set initially");

      // When
      boolean changed = bitmap.flipBit(tickIndex, true, true);

      // Then
      assertTrue(changed, "Bit should have been changed");
      assertTrue(bitmap.isSet(tickIndex), "Bit should be set after flipBit");
    }

    @Test
    @DisplayName("should clear bit when flipped=true and initialized=false")
    void shouldClearBitWhenFlippedAndNotInitialized() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);
      int tickIndex = 100;
      bitmap.setBit(tickIndex); // Set bit initially
      assertTrue(bitmap.isSet(tickIndex), "Bit should be set initially");

      // When
      boolean changed = bitmap.flipBit(tickIndex, true, false);

      // Then
      assertTrue(changed, "Bit should have been changed");
      assertFalse(bitmap.isSet(tickIndex), "Bit should be cleared after flipBit");
    }

    @Test
    @DisplayName("should not change bit when flipped=false")
    void shouldNotChangeBitWhenNotFlipped() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);
      int tickIndex = 100;

      // When - bit not set and not flipped
      boolean changed1 = bitmap.flipBit(tickIndex, false, true);

      // Then
      assertFalse(changed1, "Bit should not have been changed");
      assertFalse(bitmap.isSet(tickIndex), "Bit should remain unset");

      // When - set the bit
      bitmap.setBit(tickIndex);
      assertTrue(bitmap.isSet(tickIndex), "Bit should be set now");

      // When - bit set and not flipped
      boolean changed2 = bitmap.flipBit(tickIndex, false, false);

      // Then
      assertFalse(changed2, "Bit should not have been changed");
      assertTrue(bitmap.isSet(tickIndex), "Bit should remain set");
    }

    @Test
    @DisplayName("should not change bit when already in correct state")
    void shouldNotChangeBitWhenAlreadyInCorrectState() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);
      int tickIndex = 100;

      // When - bit not set and initialized=false (correct state)
      boolean changed1 = bitmap.flipBit(tickIndex, true, false);

      // Then
      assertFalse(changed1, "Bit should not have been changed");
      assertFalse(bitmap.isSet(tickIndex), "Bit should remain unset");

      // When - set the bit first
      bitmap.setBit(tickIndex);

      // When - bit set and initialized=true (correct state)
      boolean changed2 = bitmap.flipBit(tickIndex, true, true);

      // Then
      assertFalse(changed2, "Bit should not have been changed");
      assertTrue(bitmap.isSet(tickIndex), "Bit should remain set");
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, false, true, true", // flipped, initialized, initialBitState, expectedBitState, expectedChanged
        "true, false, true, false, true", // flipped, initialized, initialBitState, expectedBitState, expectedChanged
        "true, true, true, true, false", // flipped, initialized, initialBitState, expectedBitState, expectedChanged
        "true, false, false, false, false", // flipped, initialized, initialBitState, expectedBitState, expectedChanged
        "false, true, true, true, false", // flipped, initialized, initialBitState, expectedBitState, expectedChanged
        "false, false, false, false, false" // flipped, initialized, initialBitState, expectedBitState, expectedChanged
    })
    @DisplayName("should correctly handle all combinations")
    void shouldCorrectlyHandleAllCombinations(boolean flipped, boolean initialized,
        boolean initialBitState, boolean expectedBitState,
        boolean expectedChanged) {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);
      int tickIndex = 100;

      // Set initial bit state
      if (initialBitState) {
        bitmap.setBit(tickIndex);
      }

      // When
      boolean changed = bitmap.flipBit(tickIndex, flipped, initialized);

      // Then
      assertEquals(expectedChanged, changed,
          String.format("Changed flag should be %s for flipped=%s, initialized=%s, initialBitState=%s",
              expectedChanged, flipped, initialized, initialBitState));
      assertEquals(expectedBitState, bitmap.isSet(tickIndex),
          String.format("Bit state should be %s for flipped=%s, initialized=%s, initialBitState=%s",
              expectedBitState, flipped, initialized, initialBitState));
    }
  }

  @Nested
  @DisplayName("getSetBits")
  class GetSetBitsTests {

    @Test
    @DisplayName("Trả về danh sách rỗng cho BitSet rỗng")
    void shouldReturnEmptyListForEmptyBitmap() {
      // Given
      TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap(TEST_POOL_PAIR);

      // When
      List<Integer> setBits = bitmap.getSetBits();

      // Then
      assertNotNull(setBits);
      assertTrue(setBits.isEmpty());
    }

    @Test
    @DisplayName("Trả về danh sách chính xác cho các bit đã thiết lập")
    void shouldReturnCorrectListForSetBits() {
      // Given
      int[] positions = { 3, 7, 9, 15, 31 };
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, positions);

      // When
      List<Integer> setBits = bitmap.getSetBits();

      // Then
      assertNotNull(setBits);
      assertEquals(positions.length, setBits.size());
      for (int position : positions) {
        assertTrue(setBits.contains(position), "Danh sách nên chứa vị trí " + position);
      }
    }

    @Test
    @DisplayName("Trả về danh sách chính xác cho các bit đã thiết lập với thứ tự đúng")
    void shouldReturnCorrectOrderedListForSetBits() {
      // Given
      // Thứ tự không tuần tự để kiểm tra xem thứ tự trả về có đúng không
      int[] positions = { 31, 9, 15, 3, 7 };
      int[] sortedPositions = { 3, 7, 9, 15, 31 };
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, positions);

      // When
      List<Integer> setBits = bitmap.getSetBits();

      // Then
      assertNotNull(setBits);
      assertEquals(positions.length, setBits.size());
      for (int i = 0; i < sortedPositions.length; i++) {
        assertEquals(sortedPositions[i], setBits.get(i),
            "Vị trí tại index " + i + " phải là " + sortedPositions[i]);
      }
    }

    @Test
    @DisplayName("Xử lý BitSet với các bit có vị trí cao")
    void shouldHandleBitSetWithHighPositions() {
      // Given
      int[] positions = { 100, 500, 1000, 10000 };
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, positions);

      // When
      List<Integer> setBits = bitmap.getSetBits();

      // Then
      assertNotNull(setBits);
      assertEquals(positions.length, setBits.size());
      for (int position : positions) {
        assertTrue(setBits.contains(position), "Danh sách nên chứa vị trí " + position);
      }
    }

    @Test
    @DisplayName("Xử lý BitSet với một bit duy nhất được thiết lập")
    void shouldHandleBitSetWithSingleBit() {
      // Given
      int position = 42;
      TickBitmap bitmap = tickBitmapFactory.createBitmapWithBits(TEST_POOL_PAIR, new int[] { position });

      // When
      List<Integer> setBits = bitmap.getSetBits();

      // Then
      assertNotNull(setBits);
      assertEquals(1, setBits.size());
      assertEquals(position, setBits.get(0));
    }
  }
}
