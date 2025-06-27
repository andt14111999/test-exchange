import { v4 as uuidv4 } from "uuid";
import { UAParser } from "ua-parser-js";
import { shouldTrustDevice } from "./device-trust-preference";

// Device types
export type DeviceType = "web" | "ios" | "android";

// Generate or get existing device UUID
export function getDeviceUuid(): string {
  const DEVICE_UUID_KEY = "device_uuid";

  if (typeof window === "undefined") {
    return ""; // SSR fallback
  }

  let deviceUuid = localStorage.getItem(DEVICE_UUID_KEY);

  if (!deviceUuid) {
    // Generate new UUID
    if (crypto.randomUUID) {
      deviceUuid = crypto.randomUUID();
    } else {
      deviceUuid = uuidv4();
    }
    localStorage.setItem(DEVICE_UUID_KEY, deviceUuid);
  }

  return deviceUuid;
}

// Get device type
export function getDeviceType(): DeviceType {
  if (typeof window === "undefined") {
    return "web"; // SSR fallback
  }

  const userAgent = navigator.userAgent;

  if (/iPhone|iPad|iPod/.test(userAgent)) {
    return "ios";
  } else if (/Android/.test(userAgent)) {
    return "android";
  } else {
    return "web";
  }
}

// Get detailed device information
export function getDeviceInfo() {
  if (typeof window === "undefined") {
    return {
      browser: "Unknown",
      os: "Unknown",
      device: "Unknown",
    };
  }

  const parser = new UAParser(navigator.userAgent);
  const result = parser.getResult();

  return {
    browser: result.browser.name || "Unknown",
    os: result.os.name || "Unknown",
    device: result.device.model || "Desktop",
    platform: navigator.platform || "Unknown",
  };
}

// Generate device headers for API requests
export function getDeviceHeaders() {
  const deviceInfo = getDeviceInfo();

  const headers: Record<string, string> = {
    "Device-Uuid": getDeviceUuid(),
    "Device-Type": getDeviceType(),
    Browser: deviceInfo.browser,
    Os: deviceInfo.os,
  };

  // Add Device-Trusted header if user has preference to trust device
  if (shouldTrustDevice()) {
    headers["Device-Trusted"] = "true";
  }

  return headers;
}
