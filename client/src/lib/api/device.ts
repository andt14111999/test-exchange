import { apiClient } from "./client";
import { getDeviceHeaders } from "@/lib/utils/device";
import axios from "axios";

// Device interfaces
export interface AccessDevice {
  id: number;
  device_type: string;
  trusted: boolean;
  first_device: boolean;
  ip_address?: string;
  location?: string;
  display_name: string;
  created_at: string;
}

export interface CurrentDeviceResponse {
  id: number;
  device_type: string;
  trusted: boolean;
  first_device: boolean;
  ip_address: string;
  location: string;
  display_name: string;
  created_at: string;
}

export interface TrustDeviceResponse {
  id: number;
  trusted: boolean;
  first_device: boolean;
  message: string;
}

export interface DeviceRemoveResponse {
  message: string;
}

// API endpoints - Updated to new namespace
const DEVICE_ENDPOINTS = {
  current: "/access_devices/current",
  list: "/access_devices",
  remove: (id: number) => `/access_devices/${id}`,
} as const;

// Get current device info
export async function getCurrentDevice(): Promise<CurrentDeviceResponse> {
  try {
    const response = await apiClient.get(DEVICE_ENDPOINTS.current, {
      headers: getDeviceHeaders(),
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to get current device");
  }
}

// List all devices
export async function getAllDevices(): Promise<AccessDevice[]> {
  try {
    const response = await apiClient.get(DEVICE_ENDPOINTS.list);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to get trusted devices");
  }
}

// Get current device info (with potential trust header from getDeviceHeaders)
export async function trustCurrentDevice(): Promise<CurrentDeviceResponse> {
  try {
    // getDeviceHeaders() will include Device-Trusted: true if user has preference set
    const response = await apiClient.get(DEVICE_ENDPOINTS.current, {
      headers: getDeviceHeaders(),
    });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to get current device");
  }
}

// Remove device
export async function removeDevice(
  deviceId: number,
): Promise<DeviceRemoveResponse> {
  try {
    const response = await apiClient.delete(DEVICE_ENDPOINTS.remove(deviceId));
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      if (error.response.status === 400) {
        throw new Error(error.response.data?.message || "Cannot remove device");
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to remove device");
  }
}
