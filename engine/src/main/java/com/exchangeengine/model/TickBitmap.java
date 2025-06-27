package com.exchangeengine.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.serializer.BitSetDeserializer;
import com.exchangeengine.serializer.BitSetSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho bitmap để theo dõi các tick đã khởi tạo trong hệ thống AMM.
 * Được sử dụng để tìm tick đã khởi tạo tiếp theo một cách hiệu quả.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickBitmap {

  @NotBlank(message = "Pool pair is required")
  private String poolPair;

  @NotNull
  @JsonSerialize(using = BitSetSerializer.class)
  @JsonDeserialize(using = BitSetDeserializer.class)
  private BitSet bitmap = new BitSet();

  private long createdAt;
  private long updatedAt;

  /**
   * Constructor with pool pair
   *
   * @param poolPair the pool pair (e.g., "BTC-USDT")
   */
  public TickBitmap(String poolPair) {
    this.poolPair = poolPair;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = this.createdAt;
  }

  /**
   * Đặt bit tại index chỉ định thành 1
   */
  public BitSet setBit(int index) {
    bitmap.set(index);
    this.updatedAt = System.currentTimeMillis();
    return bitmap;
  }

  /**
   * Xóa bit tại index chỉ định (đặt về 0)
   */
  public BitSet clearBit(int index) {
    bitmap.clear(index);
    this.updatedAt = System.currentTimeMillis();
    return bitmap;
  }

  /**
   * Cập nhật bit trong bitmap dựa trên trạng thái flipped
   *
   * @param index       index của bit cần cập nhật
   * @param flipped     true nếu bit cần đổi trạng thái, false nếu giữ nguyên
   * @param initialized true nếu tick đang ở trạng thái được khởi tạo
   * @return true nếu bit đã được thay đổi, false nếu không
   */
  public boolean flipBit(int index, boolean flipped, boolean initialized) {
    boolean bitChanged = false;
    boolean currentBitState = isSet(index);

    if (flipped) {
      // Nếu flipped = true, cần đổi trạng thái bit dựa trên trạng thái initialized
      if (initialized && !currentBitState) {
        // Tick được khởi tạo nhưng bit chưa được set -> set bit
        setBit(index);
        bitChanged = true;
      } else if (!initialized && currentBitState) {
        // Tick không được khởi tạo nhưng bit đã được set -> clear bit
        clearBit(index);
        bitChanged = true;
      }
    }

    return bitChanged;
  }

  /**
   * Kiểm tra xem một bit cụ thể có được đặt hay không
   */
  public boolean isSet(int index) {
    return bitmap.get(index);
  }

  /**
   * Tìm bit đã đặt tiếp theo bắt đầu từ index đã cho
   */
  public int nextSetBit(int fromIndex) {
    return bitmap.nextSetBit(fromIndex);
  }

  /**
   * Tìm bit đã đặt trước đó trước index đã cho
   */
  public int previousSetBit(int fromIndex) {
    return bitmap.previousSetBit(fromIndex);
  }

  /**
   * Kiểm tra xem bitmap có trống không (không có bit nào được đặt thành 1)
   */
  public boolean isBitmapEmpty() {
    return bitmap.isEmpty();
  }

  /**
   * Chuyển đổi bitmap thành mảng byte để lưu trữ
   */
  public byte[] toByteArray() {
    return bitmap.toByteArray();
  }

  /**
   * Tái tạo bitmap từ mảng byte
   */
  public void fromByteArray(byte[] bytes) {
    BitSet fromBytes = BitSet.valueOf(bytes);
    this.bitmap = fromBytes;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = this.createdAt;
  }

  /**
   * Lấy kích thước hiện tại của bitmap
   */
  public int getBitmapSize() {
    return bitmap.size();
  }

  /**
   * Lấy tất cả các bit đã được set
   */
  public List<Integer> getSetBits() {
    List<Integer> setBits = new ArrayList<>();

    for (int i = bitmap.nextSetBit(0); i >= 0; i = bitmap.nextSetBit(i + 1)) {
      setBits.add(i);
    }
    return setBits;
  }

  /**
   * Xác thực rằng bitmap này có tất cả các trường bắt buộc
   */
  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<TickBitmap>> violations = validator.validate(this);

    errors.addAll(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    return errors;
  }
}
