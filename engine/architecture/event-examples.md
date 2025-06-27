# Các loại sự kiện và cách xử lý

Tài liệu này mô tả các loại sự kiện được hỗ trợ trong Exchange Engine cùng với cấu trúc JSON và các trường bắt buộc tương ứng.

## Các trường sự kiện bắt buộc

Tất cả các event được nhận vào trong Exchange Engine đều phải có những trường sau:

| Trường        | Kiểu dữ liệu | Mô tả                                                       |
| ------------- | ------------ | ----------------------------------------------------------- |
| eventId       | String       | ID của sự kiện                                              |
| operationType | String       | Loại hoạt động xem trong class OperationType                |
| actionType    | String       | Là class dưới BE sẽ được support xem trong ActionType       |
| actionId      | String       | ID của action type, dùng để theo dõi                        |
| accountKey    | String       | key của Coin Account gen ở BE `{userID}:{coin}:{accountId}` |

**Ví dụ JSON**

```json
{
  "eventId": "event-456",
  "operationType": "coin_withdrawal_releasing",
  "actionType": "CoinAccount",
  "actionId": "123",
  "accountKey": "111:btc:123",
  "timestamp": 1742379259
}
```

## 1. Tạo tài khoản coin (coin_account_create)

**Input Topic**: `EE.I.coin_account`
**Output Topic**: `EE.O.coin_account_update`

**Các trường luôn bắt buộc**:

| Trường        | Kiểu dữ liệu | Mô tả                                                       |
| ------------- | ------------ | ----------------------------------------------------------- |
| accountKey    | String       | key của Coin Account gen ở BE `{userID}:{coin}:{accountId}` |
| operationType | String       | Loại hoạt động, luôn phải là `coin_account_create`          |

**Ví dụ JSON**:

```json
{
  "eventId": "event-112233",
  "operationType": "coin_account_create",
  "actionType": "CoinAccount",
  "actionId": "123",
  "accountKey": "111:btc:123",
  "timestamp": 1742379259
}
```

## 2. Tạo Nạp tiền (coin_deposit_create)

**Input Topic**: `EE.I.coin_deposit`
**Output Topic**: `EE.O.transaction_response`

**Các trường bắt buộc**:

| Trường         | Kiểu dữ liệu | Mô tả                                           |
| -------------- | ------------ | ----------------------------------------------- |
| operationType  | String       | Loại hoạt động, bắt buộc là coin_deposit_create |
| identifier     | String       | ID định danh transaction                        |
| actionType     | String       | Loại hành động                                  |
| actionId       | String       | ID của hành động                                |
| accountKey     | String       | Key của tài khoản                               |
| status         | String       | Trạng thái chỉ nhận trạng thái pending          |
| amount         | BigDecimal   | Số tiền ( >0 )                                  |
| coin           | String       | Loại coin chỉ hỗ trợ coin trong CoinType        |
| txHash         | String       | Hash của giao dịch                              |
| layer          | String       | Layer của blockchain                            |
| depositAddress | String       | Địa chỉ nạp tiền                                |

**Ví dụ JSON**:

```json
{
  "coin": "USDT",
  "amount": 100.0,
  "actionType": "CoinTransaction",
  "operationType": "coin_deposit_create",
  "actionId": "action-456",
  "eventId": "event-789",
  "identifier": "id-012",
  "accountKey": "111:btc:123",
  "status": "pending",
  "txHash": "tx-abcdef1234567890",
  "layer": "sol",
  "depositAddress": "address-0x123456789abcdef",
  "timestamp": 1742379259
}
```

## 3. Tạo Rút tiền (coin_withdrawal_create)

**Input Topic**: `EE.I.coin_withdraw`
**Output Topic**: `EE.O.transaction_response`

**Các trường bắt buộc**:

| Trường             | Kiểu dữ liệu | Mô tả                                                                       |
| ------------------ | ------------ | --------------------------------------------------------------------------- |
| operationType      | String       | Loại hoạt động, bắt buộc là coin_withdrawal_create                          |
| identifier         | String       | ID định danh transaction                                                    |
| actionType         | String       | Loại hành động                                                              |
| actionId           | String       | ID của hành động                                                            |
| accountKey         | String       | Key của tài khoản                                                           |
| status             | String       | Trạng thái là completed, failed, cancelled hoặc verified, verified sẽ thực hiện đóng băng số dư |
| amount             | BigDecimal   | Số tiền ( >0 )                                                              |
| coin               | String       | Loại coin chỉ hỗ trợ coin trong CoinType                                    |
| txHash             | String       | Hash của giao dịch                                                          |
| layer              | String       | Layer của blockchain                                                        |
| destinationAddress | String       | Địa chỉ rút tiền                                                            |
| fee                | BigDecimal   | fee thu của user                                                            |

**Ví dụ JSON**:

```json
{
  "coin": "USDT",
  "amount": 100.0,
  "actionType": "CoinTransaction",
  "operationType": "coin_withdrawal_create",
  "actionId": "action-456",
  "eventId": "event-789",
  "identifier": "id-012",
  "accountKey": "111:btc:123",
  "status": "pending",
  "txHash": "tx-abcdef1234567890",
  "layer": "sol",
  "destinationAddress": "address-0x123456789abcdef",
  "fee": 0.1,
  "timestamp": 1742379259
}
```

## 4. Hoàn thành rút tiền (coin_withdrawal_releasing)

`Status của transaction phải là processing`

**Input Topic**: `EE.I.coin_withdraw`
**Output Topic**: `EE.O.transaction_response`

**Các trường bắt buộc**:

| Trường        | Kiểu dữ liệu | Mô tả                                                 |
| ------------- | ------------ | ----------------------------------------------------- |
| operationType | String       | Loại hoạt động, bắt buộc là coin_withdrawal_releasing |
| identifier    | String       | ID định danh transaction                              |

**Ví dụ JSON**:

```json
{
  "eventId": "event-789",
  "operationType": "coin_withdrawal_releasing",
  "actionType": "CoinTransaction",
  "actionId": "action-456",
  "identifier": "id-012",
  "timestamp": 1742379259
}
```

## 5. Hủy lệnh rút tiền (coin_withdrawal_failed)

`Status của transaction phải là processing`

**Input Topic**: `EE.I.coin_withdraw`
**Output Topic**: `EE.O.transaction_response`

**Các trường bắt buộc**:

| Trường        | Kiểu dữ liệu | Mô tả                                              |
| ------------- | ------------ | -------------------------------------------------- |
| operationType | String       | Loại hoạt động, bắt buộc là coin_withdrawal_failed |
| identifier    | String       | ID định danh transaction                           |

**Ví dụ JSON**:

```json
{
  "eventId": "event-789",
  "operationType": "coin_withdrawal_failed",
  "actionType": "CoinTransaction",
  "actionId": "action-456",
  "identifier": "id-012",
  "timestamp": 1742379259
}
```

## 6. Truy vấn số dư (balance_query)

**Input Topic**: `EE.I.coin_account_query`
**Output Topic**: `EE.O.coin_account_update`

**Các trường bắt buộc**:

| Trường        | Kiểu dữ liệu | Mô tả                                     |
| ------------- | ------------ | ----------------------------------------- |
| operationType | String       | Loại hoạt động, bắt buộc là balance_query |
| accountKey    | String       | Key của tài khoản                         |

**Ví dụ JSON**:

```json
{
  "eventId": "event-789",
  "operationType": "coin_withdrawal_failed",
  "actionType": "CoinTransaction",
  "actionId": "action-456",
  "accountKey": "111:btc:123",
  "timestamp": 1742379259
}
```

## 7. Reset số dư (coin_account_reset)

`Chỉ dành cho môi trường development`

**Input Topic**: `EE.I.coin_account_reset`
**Output Topic**: `EE.O.transaction_response`

**Các trường bắt buộc**:

| Trường     | Kiểu dữ liệu | Mô tả                                            |
| ---------- | ------------ | ------------------------------------------------ |
| accountKey | String       | Khóa tài khoản (có thể là null nếu reset tất cả) |

**Ví dụ JSON**:

```json
{
  "accountKey": "USDT:user123",
  "timestamp": 1625097600000
}
```

## 8. Tạo AMM Pool (amm_pool_create)

**Input Topic**: `EE.I.amm_pool`
**Output Topic**: `EE.O.amm_pool_response`

**Các trường bắt buộc**:

| Trường                | Kiểu dữ liệu | Mô tả                                       |
| --------------------- | ------------ | ------------------------------------------- |
| eventId               | String       | ID của sự kiện                              |
| operationType         | String       | Loại hoạt động, bắt buộc là amm_pool_create |
| actionType            | String       | Loại hành động, phải là AmmPool             |
| actionId              | String       | ID của hành động                            |
| pair                  | String       | Cặp tiền tệ của pool (ví dụ: BTC/USDT)      |
| token0                | String       | Token đầu tiên trong pair                   |
| token1                | String       | Token thứ hai trong pair                    |
| tickSpacing           | int          | Khoảng cách giữa các tick                   |
| feePercentage         | double       | Tỷ lệ phí giao dịch                         |
| feeProtocolPercentage | double       | Tỷ lệ phí protocol                          |
| isActive              | boolean      | Trạng thái hoạt động của pool               |
| initPrice             | BigDecimal   | Giá ban đầu của token0/token1 (tùy chọn)    |

**Ví dụ JSON**:

```json
{
  "eventId": "event-123456",
  "operationType": "amm_pool_create",
  "actionType": "AmmPool",
  "actionId": "action-789",
  "pair": "BTC/USDT",
  "token0": "BTC",
  "token1": "USDT",
  "tickSpacing": 10,
  "feePercentage": 0.3,
  "feeProtocolPercentage": 0.1,
  "isActive": true,
  "initPrice": 50000.0,
  "timestamp": 1742379259
}
```

## 9. Cập nhật AMM Pool (amm_pool_update)

**Input Topic**: `EE.I.amm_pool`
**Output Topic**: `EE.O.amm_pool_response`

**Các trường bắt buộc**:

| Trường                | Kiểu dữ liệu | Mô tả                                        |
| --------------------- | ------------ | -------------------------------------------- |
| eventId               | String       | ID của sự kiện                               |
| operationType         | String       | Loại hoạt động, bắt buộc là amm_pool_update  |
| actionType            | String       | Loại hành động, phải là AmmPool              |
| actionId              | String       | ID của hành động                             |
| pair                  | String       | Cặp tiền tệ của pool cần cập nhật            |
| feePercentage         | double       | Tỷ lệ phí giao dịch mới                      |
| feeProtocolPercentage | double       | Tỷ lệ phí protocol mới                       |
| isActive              | boolean      | Trạng thái hoạt động mới của pool            |
| initPrice             | BigDecimal   | Giá ban đầu mới của token0/token1 (tùy chọn) |

**Hạn chế khi cập nhật initPrice**:

- Không thể cập nhật initPrice nếu pool đang hoạt động (isActive = true)
- Không thể cập nhật initPrice nếu pool đã có thanh khoản (totalValueLockedToken0 > 0 hoặc totalValueLockedToken1 > 0)
- Giá trị initPrice mới phải lớn hơn 0

**Ví dụ JSON**:

```json
{
  "eventId": "event-234567",
  "operationType": "amm_pool_update",
  "actionType": "AmmPool",
  "actionId": "action-890",
  "pair": "BTC/USDT",
  "feePercentage": 0.4,
  "feeProtocolPercentage": 0.15,
  "isActive": false,
  "initPrice": 48000.0,
  "timestamp": 1742380000
}
```

## 10. Tạo AMM Position (amm_position_create)

**Input Topic**: `EE.I.amm_position`
**Output Topic**: `EE.O.amm_position_response`

**Các trường bắt buộc**:

| Trường           | Kiểu dữ liệu | Mô tả                                           |
| ---------------- | ------------ | ----------------------------------------------- |
| eventId          | String       | ID của sự kiện                                  |
| operationType    | String       | Loại hoạt động, bắt buộc là amm_position_create |
| actionType       | String       | Loại hành động, phải là AmmPosition             |
| actionId         | String       | ID của hành động                                |
| identifier       | String       | ID định danh vị thế AMM                         |
| poolPair         | String       | Cặp tiền tệ của pool (ví dụ: BTC/USDT)          |
| ownerAccountKey0 | String       | Khóa tài khoản sở hữu token0                    |
| ownerAccountKey1 | String       | Khóa tài khoản sở hữu token1                    |
| tickLowerIndex   | int          | Chỉ số tick thấp nhất của vị thế                |
| tickUpperIndex   | int          | Chỉ số tick cao nhất của vị thế                 |
| amount0Initial   | BigDecimal   | Số lượng token0 ban đầu để cung cấp thanh khoản |
| amount1Initial   | BigDecimal   | Số lượng token1 ban đầu để cung cấp thanh khoản |
| slippage         | BigDecimal   | Tỷ lệ trượt giá cho phép (ví dụ: 0.01 = 1%)     |

**Ví dụ JSON**:

```json
{
  "eventId": "event-pos-123456",
  "operationType": "amm_position_create",
  "actionType": "AmmPosition",
  "actionId": "action-pos-789",
  "identifier": "position-001",
  "poolPair": "BTC/USDT",
  "ownerAccountKey0": "111:btc:123",
  "ownerAccountKey1": "111:usdt:123",
  "tickLowerIndex": -100,
  "tickUpperIndex": 100,
  "amount0Initial": 0.5,
  "amount1Initial": 25000.0,
  "slippage": 0.01,
  "timestamp": 1742380500
}
```

## 11. Thu phí từ AMM Position (amm_position_collect)

**Input Topic**: `EE.I.amm_position`
**Output Topic**: `EE.O.amm_position_response`

**Các trường bắt buộc**:

| Trường           | Kiểu dữ liệu | Mô tả                                            |
| ---------------- | ------------ | ------------------------------------------------ |
| eventId          | String       | ID của sự kiện                                   |
| operationType    | String       | Loại hoạt động, bắt buộc là amm_position_collect |
| actionType       | String       | Loại hành động, phải là AmmPosition              |
| actionId         | String       | ID của hành động                                 |
| identifier       | String       | ID định danh vị thế AMM cần thu phí              |
| ownerAccountKey0 | String       | Khóa tài khoản nhận phí token0                   |
| ownerAccountKey1 | String       | Khóa tài khoản nhận phí token1                   |

**Ví dụ JSON**:

```json
{
  "eventId": "event-collect-123456",
  "operationType": "amm_position_collect",
  "actionType": "AmmPosition",
  "actionId": "action-collect-789",
  "identifier": "position-001",
  "ownerAccountKey0": "111:btc:123",
  "ownerAccountKey1": "111:usdt:123",
  "timestamp": 1742386500
}
```

## 12. Đóng AMM Position (amm_position_close)

**Input Topic**: `EE.I.amm_position`
**Output Topic**: `EE.O.amm_position_response`

**Các trường bắt buộc**:

| Trường           | Kiểu dữ liệu | Mô tả                                          |
| ---------------- | ------------ | ---------------------------------------------- |
| eventId          | String       | ID của sự kiện                                 |
| operationType    | String       | Loại hoạt động, bắt buộc là amm_position_close |
| actionType       | String       | Loại hành động, phải là AmmPosition            |
| actionId         | String       | ID của hành động                               |
| identifier       | String       | ID định danh vị thế AMM cần đóng               |
| ownerAccountKey0 | String       | Khóa tài khoản nhận lại token0                 |
| ownerAccountKey1 | String       | Khóa tài khoản nhận lại token1                 |

**Ví dụ JSON**:

```json
{
  "eventId": "event-close-123456",
  "operationType": "amm_position_close",
  "actionType": "AmmPosition",
  "actionId": "action-close-789",
  "identifier": "position-001",
  "ownerAccountKey0": "111:btc:123",
  "ownerAccountKey1": "111:usdt:123",
  "timestamp": 1742390500
}
```

## Cấu trúc phản hồi

### 1. Phản hồi cập nhật tài khoản

**Output Topic**: `EE.O.coin_account_update`

**Các trường**:

| Trường           | Kiểu dữ liệu | Mô tả                       |
| ---------------- | ------------ | --------------------------- |
| messageId        | String       | message id                  |
| key              | String       | Account key                 |
| availableBalance | Bigdecimal   | Số dư khả dụng              |
| frozenBalance    | Bigdecimal   | Số dư bị đóng băng          |
| totalBalance     | Bigdecimal   | Tổng số balance             |
| createdAt        | Long         | Thời gian tạo               |
| updatedAt        | Long         | Thời gian cập nhất mới nhất |

**Ví dụ JSON**:

```json
{
  "messageId": "4a0b8a99-01f1-426f-8bb0-b1fda506fc82",
  "key": "coin:usdt:user1",
  "availableBalance": 7527.0,
  "frozenBalance": 111.0,
  "createdAt": 1742375689711,
  "updatedAt": 1742384775899,
  "totalBalance": 7638.0
}
```

### 2. Phản hồi kết quả giao dịch

**Output Topic**: `EE.O.transaction_response`

**Các trường**:

| Trường       | Kiểu dữ liệu | Mô tả                                                                                          |
| ------------ | ------------ | ---------------------------------------------------------------------------------------------- |
| messageId    | String       | message id                                                                                     |
| eventID      | String       | Event ID mà BE gửi lên                                                                         |
| isSuccess    | Boolean      | Thành công                                                                                     |
| errorMessage | String       | Message lỗi                                                                                    |
| object       | Object       | là Object model được service này xử lý, hãy xem các field ở model hoặc tìm hàm `toMessageJson` |
| timestamp    | Long         | Thời gian gửi                                                                                  |

**Ví dụ JSON**:

```json
{
  "messageId": "5aace824-5f56-47bf-b81f-040b862e8d27",
  "eventId": "4bd8f3ad-7b57-4897-8afd-2bf2de6e38aa",
  "errorMessage": null,
  "isSuccess": false,
  "object": {
    "actionType": "COIN_TRANSACTION",
    "actionId": "2dfd6f37-c686-4569-8749-4b9434a91174",
    "accountKey": "coin:usdt:user1",
    "identifier": "withdraw-12345678002",
    "status": "completed",
    "statusExplanation": "",
    "createdAt": 1742382462265,
    "updatedAt": 1742384775899,
    "amount": 111.0,
    "coin": "usdt",
    "txHash": "tx-bf9f1f2e350cfa614ff11d7dcc07373d",
    "layer": "L1",
    "destinationAddress": "address-5923df58bdce500197eb3eeb55c46194",
    "fee": 0
  },
  "timestamp": 1742384715256
}
```

### 3. Phản hồi AMM Pool

**Output Topic**: `EE.O.amm_pool_response`

**Các trường**:

| Trường       | Kiểu dữ liệu | Mô tả                                    |
| ------------ | ------------ | ---------------------------------------- |
| messageId    | String       | message id                               |
| eventID      | String       | Event ID mà BE gửi lên                   |
| isSuccess    | Boolean      | Thành công                               |
| errorMessage | String       | Message lỗi                              |
| object       | Object       | Thông tin của AMM Pool được tạo/cập nhật |
| timestamp    | Long         | Thời gian gửi                            |

**Ví dụ JSON**:

```json
{
  "messageId": "c4e8d2a1-9f56-42bf-b81f-040b862e8d27",
  "eventId": "event-123456",
  "errorMessage": null,
  "isSuccess": true,
  "object": {
    "pair": "BTC/USDT",
    "token0": "BTC",
    "token1": "USDT",
    "tickSpacing": 10,
    "feePercentage": 0.3,
    "feeProtocolPercentage": 0.1,
    "isActive": true,
    "createdAt": 1742379259000,
    "updatedAt": 1742379259000
  },
  "timestamp": 1742379260000
}
```

### 4. Phản hồi AMM Position

**Output Topic**: `EE.O.amm_position_response`

**Các trường**:

| Trường       | Kiểu dữ liệu | Mô tả                                 |
| ------------ | ------------ | ------------------------------------- |
| messageId    | String       | message id                            |
| eventID      | String       | Event ID mà BE gửi lên                |
| isSuccess    | Boolean      | Thành công                            |
| errorMessage | String       | Message lỗi                           |
| object       | Object       | Thông tin của AMM Position được xử lý |
| timestamp    | Long         | Thời gian gửi                         |

**Ví dụ JSON cho tạo vị thế**:

```json
{
  "messageId": "d5f9e2b1-8f67-42bf-c92e-051c973f9d38",
  "eventId": "event-pos-123456",
  "errorMessage": null,
  "isSuccess": true,
  "object": {
    "identifier": "position-001",
    "poolPair": "BTC/USDT",
    "ownerAccountKey0": "111:btc:123",
    "ownerAccountKey1": "111:usdt:123",
    "tickLowerIndex": -100,
    "tickUpperIndex": 100,
    "liquidity": 1255478964,
    "amount0": 0.499,
    "amount1": 24950,
    "feeGrowthInside0LastX128": 0,
    "feeGrowthInside1LastX128": 0,
    "tokensOwed0": 0,
    "tokensOwed1": 0,
    "createdAt": 1742380500000,
    "updatedAt": 1742380500000
  },
  "timestamp": 1742380501000
}
```

**Ví dụ JSON cho thu phí**:

```json
{
  "messageId": "e6f8g3c2-7h56-31bf-d83f-062d973f9e49",
  "eventId": "event-collect-123456",
  "errorMessage": null,
  "isSuccess": true,
  "object": {
    "identifier": "position-001",
    "tokensCollected0": 0.01,
    "tokensCollected1": 500.0,
    "updatedAt": 1742386500000
  },
  "timestamp": 1742386501000
}
```

**Ví dụ JSON cho đóng vị thế**:

```json
{
  "messageId": "f7g9h4d3-6i45-20bf-e94g-073e973f9f50",
  "eventId": "event-close-123456",
  "errorMessage": null,
  "isSuccess": true,
  "object": {
    "identifier": "position-001",
    "amount0Returned": 0.48,
    "amount1Returned": 24000.0,
    "tokensFee0": 0.025,
    "tokensFee1": 1250.0,
    "status": "closed",
    "closedAt": 1742390500000
  },
  "timestamp": 1742390501000
}
```

## Xử lý lỗi

Khi gặp lỗi, Exchange Engine sẽ trả về một phản hồi lỗi với cấu trúc như sau:

**Các trường**:

| Trường       | Kiểu dữ liệu | Mô tả                            |
| ------------ | ------------ | -------------------------------- |
| errorCode    | String       | Mã lỗi                           |
| errorMessage | String       | Thông báo lỗi chi tiết           |
| timestamp    | Long         | Thời gian tính bằng milliseconds |

**Ví dụ JSON**:

```json
{
  "errorCode": "INSUFFICIENT_BALANCE",
  "errorMessage": "Insufficient balance for withdrawal",
  "timestamp": 1625097600000
}
```

**Các mã lỗi phổ biến**:

| Mã lỗi                  | Mô tả                     |
| ----------------------- | ------------------------- |
| INSUFFICIENT_BALANCE    | Số dư không đủ            |
| ACCOUNT_NOT_FOUND       | Không tìm thấy tài khoản  |
| INVALID_AMOUNT          | Số tiền không hợp lệ      |
| VALIDATION_ERROR        | Lỗi xác thực dữ liệu      |
| INTERNAL_ERROR          | Lỗi nội bộ                |
| POOL_NOT_FOUND          | Không tìm thấy AMM Pool   |
| INVALID_POOL_PAIR       | Cặp token không hợp lệ    |
| POSITION_NOT_FOUND      | Không tìm thấy vị thế     |
| INVALID_TICK_RANGE      | Phạm vi tick không hợp lệ |
| POSITION_ALREADY_CLOSED | Vị thế đã được đóng       |
| INVALID_SLIPPAGE        | Độ trượt giá không hợp lệ |
| NO_FEES_TO_COLLECT      | Không có phí để thu       |
