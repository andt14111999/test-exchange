# 2FA và Trusted Device Integration Guide

## Overview

Hệ thống 2FA (Two-Factor Authentication) và Trusted Device được implement để bảo mật các thao tác nhạy cảm như withdrawal. Khi user có 2FA enabled, họ cần nhập 2FA code hoặc sử dụng trusted device.

## Required Headers

### 1. Device-Uuid Header

**Bắt buộc** cho tất cả requests liên quan đến device management và 2FA bypass.

```javascript
headers: {
  'Device-Uuid': 'unique-device-identifier', // UUID duy nhất cho device
  'Device-Type': 'web|ios|android',          // Loại device
  'Authorization': 'Bearer <token>',
  // Optional device info headers
  'Browser': 'Chrome',
  'Os': 'macOS'
}
```

### 2. Device-Type Header

Xác định loại device để phân biệt web/mobile:

- `web` - Web browser
- `ios` - iOS app
- `android` - Android app

## API Endpoints

### 1. Get Current Device Info

```javascript
GET /api/v1/users/access_devices/current
Headers: {
  'Device-Uuid': 'device-uuid',
  'Device-Type': 'web',
  'Authorization': 'Bearer <token>'
}

Response:
{
  "id": 123,
  "device_type": "web",
  "trusted": true,
  "first_device": true,
  "ip_address": "192.168.1.1",
  "location": "Ho Chi Minh City, Vietnam",
  "display_name": "Chrome (macOS)",
  "created_at": "2025-06-24T04:00:00.000Z"
}
```

### 2. List Trusted Devices

```javascript
GET /api/v1/users/access_devices
Headers: {
  'Authorization': 'Bearer <token>'
}

Response: [
  {
    "id": 123,
    "device_type": "web",
    "trusted": true,
    "first_device": true,
    "display_name": "Chrome (macOS)",
    "location": "Ho Chi Minh City, Vietnam",
    "created_at": "2025-06-24T04:00:00.000Z"
  }
]
```

### 3. Trust Current Device

```javascript
POST /api/v1/users/access_devices/trust
Headers: {
  'Device-Uuid': 'device-uuid',
  'Device-Type': 'web',
  'Authorization': 'Bearer <token>'
}

Response:
{
  "id": 123,
  "trusted": true,
  "first_device": true,
  "message": "Device marked as trusted"
}
```

### 4. Remove Trusted Device

```javascript
DELETE /api/v1/users/access_devices/{device_id}
Headers: {
  'Authorization': 'Bearer <token>'
}

Success Response:
{
  "message": "Device removed successfully"
}

Error Response (cannot remove last first device):
{
  "message": "Cannot remove the only first device",
  "status": "error"
}
```

## Coin Withdrawal với 2FA

### Basic Request Structure

```javascript
POST /api/v1/coin_withdrawals
Headers: {
  'Device-Uuid': 'device-uuid',      // Bắt buộc
  'Device-Type': 'web',              // Bắt buộc
  'Authorization': 'Bearer <token>'
}
Body: {
  "coin_address": "recipient-address",
  "coin_amount": 100.0,
  "coin_currency": "usdt",
  "coin_layer": "main",
  "two_factor_code": "123456"        // Chỉ cần khi yêu cầu 2FA
}
```

## Response Scenarios

### 1. User không có 2FA

```javascript
// Request không cần two_factor_code
Response:
{
  "status": "success",
  "data": {
    "id": 456,
    "coin_amount": "100.0",
    "status": "pending"
  }
}
```

### 2. User có 2FA + Device chưa trusted

```javascript
// Request không có two_factor_code
Response (400 Bad Request):
{
  "message": "2FA code is required for this action",
  "requires_2fa": true,
  "device_trusted": false
}

// Frontend cần show 2FA input và gửi lại request với two_factor_code
```

### 3. User có 2FA + 2FA code sai

```javascript
Response (400 Bad Request):
{
  "message": "Invalid 2FA code"
}

// Frontend show error và cho user nhập lại
```

### 4. User có 2FA + 2FA code đúng

```javascript
Response:
{
  "status": "success",
  "data": {
    "id": 456,
    "coin_amount": "100.0",
    "status": "pending"
  }
}

// Device sẽ được tự động tạo và trust sau khi 2FA thành công
```

### 5. User có 2FA + Device đã trusted

```javascript
// Request không cần two_factor_code
Response:
{
  "status": "success",
  "data": {
    "id": 456,
    "coin_amount": "100.0",
    "status": "pending"
  }
}
```

## Frontend Implementation Flow

### 1. Device UUID Generation

```javascript
// Generate hoặc retrieve device UUID
let deviceUuid = localStorage.getItem("device_uuid");
if (!deviceUuid) {
  deviceUuid = generateUUID(); // Sử dụng crypto.randomUUID() hoặc uuid library
  localStorage.setItem("device_uuid", deviceUuid);
}
```

### 2. Withdrawal Request Handler

```javascript
async function submitWithdrawal(withdrawalData, twoFactorCode = null) {
  const headers = {
    "Device-Uuid": getDeviceUuid(),
    "Device-Type": getDeviceType(), // 'web', 'ios', 'android'
    Authorization: `Bearer ${getAuthToken()}`,
    Browser: navigator.userAgent.includes("Chrome") ? "Chrome" : "Other",
    Os: getOperatingSystem(),
  };

  const body = {
    ...withdrawalData,
    ...(twoFactorCode && { two_factor_code: twoFactorCode }),
  };

  try {
    const response = await fetch("/api/v1/coin_withdrawals", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...headers,
      },
      body: JSON.stringify(body),
    });

    const data = await response.json();

    if (response.ok) {
      // Success - withdrawal created
      showSuccess("Withdrawal submitted successfully");
      return data;
    } else {
      // Handle errors
      handleWithdrawalError(data, withdrawalData);
    }
  } catch (error) {
    showError("Network error occurred");
  }
}

function handleWithdrawalError(errorData, originalWithdrawalData) {
  if (errorData.requires_2fa && !errorData.device_trusted) {
    // Show 2FA input modal
    show2FAModal((twoFactorCode) => {
      submitWithdrawal(originalWithdrawalData, twoFactorCode);
    });
  } else if (errorData.message === "Invalid 2FA code") {
    // Show error và cho nhập lại
    showError("Invalid 2FA code. Please try again.");
    show2FAModal((twoFactorCode) => {
      submitWithdrawal(originalWithdrawalData, twoFactorCode);
    });
  } else {
    // Other errors
    showError(errorData.message || "An error occurred");
  }
}
```

### 3. Device Management UI

```javascript
// Load và display trusted devices
async function loadTrustedDevices() {
  const response = await fetch("/api/v1/users/access_devices", {
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
    },
  });

  const devices = await response.json();
  displayDeviceList(devices);
}

// Trust current device
async function trustCurrentDevice() {
  const response = await fetch("/api/v1/users/access_devices/trust", {
    method: "POST",
    headers: {
      "Device-Uuid": getDeviceUuid(),
      "Device-Type": getDeviceType(),
      Authorization: `Bearer ${getAuthToken()}`,
    },
  });

  if (response.ok) {
    showSuccess("Device marked as trusted");
    loadTrustedDevices(); // Refresh list
  }
}

// Remove device
async function removeDevice(deviceId) {
  const response = await fetch(`/api/v1/users/access_devices/${deviceId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${getAuthToken()}`,
    },
  });

  const data = await response.json();

  if (response.ok) {
    showSuccess("Device removed successfully");
    loadTrustedDevices(); // Refresh list
  } else {
    showError(data.message);
  }
}
```

### 4. Utility Functions

```javascript
function generateUUID() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }
  // Fallback for older browsers
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function getDeviceType() {
  // Detect device type
  if (/iPhone|iPad|iPod/.test(navigator.userAgent)) {
    return "ios";
  } else if (/Android/.test(navigator.userAgent)) {
    return "android";
  } else {
    return "web";
  }
}

function getOperatingSystem() {
  const platform = navigator.platform;
  if (platform.includes("Mac")) return "macOS";
  if (platform.includes("Win")) return "Windows";
  if (platform.includes("Linux")) return "Linux";
  return "Unknown";
}

function getDeviceUuid() {
  return localStorage.getItem("device_uuid");
}

function getAuthToken() {
  return localStorage.getItem("auth_token");
}
```

## Error Handling Best Practices

### 1. Network Errors

```javascript
catch (error) {
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    showError('Network connection error. Please check your internet.');
  } else {
    showError('An unexpected error occurred.');
  }
}
```

### 2. API Error Responses

```javascript
if (!response.ok) {
  const errorData = await response.json();

  switch (response.status) {
    case 400:
      if (errorData.requires_2fa) {
        handle2FARequired(errorData);
      } else {
        showError(errorData.message);
      }
      break;
    case 401:
      redirectToLogin();
      break;
    case 404:
      showError("Resource not found");
      break;
    case 500:
      showError("Server error. Please try again later.");
      break;
    default:
      showError(errorData.message || "An error occurred");
  }
}
```

## Security Notes

1. **Device UUID**: Phải persist trong localStorage và không được thay đổi trừ khi user clear data
2. **2FA Code**: Chỉ gửi khi cần thiết, không store ở client
3. **Headers**: Device-Uuid và Device-Type là required cho device management
4. **Trust Logic**: Device được trust tự động sau 72 giờ hoặc khi được mark manually
5. **First Device**: Device đầu tiên của user được trust ngay lập tức

## Testing

### Test Cases cần implement:

1. Withdrawal với user không có 2FA
2. Withdrawal với user có 2FA + device chưa trust
3. Withdrawal với user có 2FA + device đã trust
4. Invalid 2FA code handling
5. Device management (list, trust, remove)
6. Network error handling
7. Device UUID persistence across sessions
