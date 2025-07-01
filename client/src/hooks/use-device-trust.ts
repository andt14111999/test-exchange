import { useState, useEffect, useCallback } from "react";
import { getCurrentDevice } from "@/lib/api/device";
import { useUserStore } from "@/lib/store/user-store";

export interface DeviceTrustState {
  isDeviceTrusted: boolean;
  isCheckingDevice: boolean;
  setIsDeviceTrusted: (trusted: boolean) => void;
  recheckDeviceTrust: () => Promise<void>;
}

/**
 * Custom hook to manage device trust status for 2FA
 * Returns device trust state and utilities to update it
 */
export function useDeviceTrust(): DeviceTrustState {
  const { user } = useUserStore();
  const [isDeviceTrusted, setIsDeviceTrusted] = useState<boolean>(false);
  const [isCheckingDevice, setIsCheckingDevice] = useState<boolean>(true);

  const checkDeviceTrust = useCallback(async () => {
    // If user doesn't have 2FA enabled, consider device as trusted
    if (!user?.authenticatorEnabled) {
      setIsDeviceTrusted(true);
      setIsCheckingDevice(false);
      return;
    }

    try {
      const deviceInfo = await getCurrentDevice();
      setIsDeviceTrusted(deviceInfo.trusted);
    } catch (error) {
      console.warn("Failed to check device trust status:", error);
      // Default to not trusted if check fails (safer approach)
      setIsDeviceTrusted(false);
    } finally {
      setIsCheckingDevice(false);
    }
  }, [user?.authenticatorEnabled]);

  // Check device trust status when component mounts or user 2FA status changes
  useEffect(() => {
    setIsCheckingDevice(true);
    checkDeviceTrust();
  }, [checkDeviceTrust]);

  // Function to manually recheck device trust (useful after trusting device)
  const recheckDeviceTrust = async () => {
    setIsCheckingDevice(true);
    await checkDeviceTrust();
  };

  return {
    isDeviceTrusted,
    isCheckingDevice,
    setIsDeviceTrusted,
    recheckDeviceTrust,
  };
}
