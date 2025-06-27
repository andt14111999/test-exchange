/**
 * Device Trust Preference Management
 * Manages user's preference to trust current device
 */

const DEVICE_TRUST_PREFERENCE_KEY = "device_trust_preference";

export interface DeviceTrustPreference {
  trustDevice: boolean;
  deviceUuid: string;
  timestamp: number;
}

/**
 * Get user's device trust preference from localStorage
 */
export function getDeviceTrustPreference(): boolean {
  if (typeof window === "undefined") return false;

  try {
    const stored = localStorage.getItem(DEVICE_TRUST_PREFERENCE_KEY);
    if (!stored) return false;

    const preference: DeviceTrustPreference = JSON.parse(stored);

    // Check if preference is for current device
    const currentDeviceUuid = localStorage.getItem("device_uuid");
    if (preference.deviceUuid !== currentDeviceUuid) {
      return false;
    }

    // Check if preference is not too old (optional: expire after 30 days)
    const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000;
    if (preference.timestamp < thirtyDaysAgo) {
      clearDeviceTrustPreference();
      return false;
    }

    return preference.trustDevice;
  } catch (error) {
    console.warn("Failed to get device trust preference:", error);
    return false;
  }
}

/**
 * Set user's device trust preference
 */
export function setDeviceTrustPreference(trustDevice: boolean): void {
  if (typeof window === "undefined") return;

  try {
    const deviceUuid = localStorage.getItem("device_uuid");
    if (!deviceUuid) {
      console.warn("Device UUID not found, cannot set trust preference");
      return;
    }

    const preference: DeviceTrustPreference = {
      trustDevice,
      deviceUuid,
      timestamp: Date.now(),
    };

    localStorage.setItem(
      DEVICE_TRUST_PREFERENCE_KEY,
      JSON.stringify(preference),
    );
  } catch (error) {
    console.warn("Failed to set device trust preference:", error);
  }
}

/**
 * Clear device trust preference
 */
export function clearDeviceTrustPreference(): void {
  if (typeof window === "undefined") return;

  try {
    localStorage.removeItem(DEVICE_TRUST_PREFERENCE_KEY);
  } catch (error) {
    console.warn("Failed to clear device trust preference:", error);
  }
}

/**
 * Check if user wants to trust device for current action
 * This is used to determine whether to send Device-Trusted header
 */
export function shouldTrustDevice(): boolean {
  return getDeviceTrustPreference();
}
