import { UserData } from "@/types/user";
import { apiClient } from "./client";
import { API_ENDPOINTS } from "./config";
import axios from "axios";

export async function fetchUserData(): Promise<UserData> {
  try {
    const response = await apiClient.get(API_ENDPOINTS.users.me);

    // Handle different response formats
    if (response.data?.data) {
      return response.data.data;
    }

    if (response.data) {
      return response.data;
    }

    throw new Error("Invalid response format");
  } catch (error) {
    // Handle specific error types differently
    if (axios.isAxiosError(error) && error.response) {
      // Handle HTTP errors
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      throw new Error(`API error: ${error.response.status}`);
    }

    if (error instanceof Error) {
      throw error; // Rethrow original error
    }

    throw new Error("Failed to fetch user data");
  }
}

export async function updateUsername(username: string): Promise<UserData> {
  try {
    // Gọi API endpoint
    const response = await apiClient.patch(API_ENDPOINTS.users.updateUsername, {
      username, // Đảm bảo tên tham số này khớp với tên trường API mong đợi
    });

    // Handle different response formats
    let userData: UserData;
    if (response.data?.data) {
      userData = response.data.data;
    } else if (response.data) {
      userData = response.data;
    } else {
      throw new Error("Invalid response format");
    }

    // Nếu userData không có username, thêm vào từ input
    if (!userData.username) {
      userData.username = username;
    }

    return userData;
  } catch (error) {
    // Handle specific error types differently
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      if (error.response.status === 422 && error.response.data?.errors) {
        throw new Error(error.response.data.errors.join(", "));
      }
      throw new Error(`API error: ${error.response.status}`);
    }

    if (error instanceof Error) {
      throw error; // Rethrow original error
    }

    throw new Error("Failed to update username");
  }
}

// 2FA API functions
export interface TwoFactorAuthStatus {
  enabled: boolean;
}

export interface TwoFactorAuthEnable {
  qr_code_uri: string;
  message: string;
}

export interface TwoFactorAuthResponse {
  message: string;
}

export async function enableTwoFactorAuth(): Promise<TwoFactorAuthEnable> {
  try {
    const response = await apiClient.post(
      API_ENDPOINTS.users.twoFactorAuth.enable,
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      if (error.response.status === 400) {
        throw new Error(
          error.response.data?.message || "2FA is already enabled",
        );
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to enable 2FA");
  }
}

export async function verifyTwoFactorAuth(
  code: string,
  trustDevice: boolean = false,
): Promise<TwoFactorAuthResponse> {
  try {
    const response = await apiClient.post(
      API_ENDPOINTS.users.twoFactorAuth.verify,
      {
        code,
      },
      {
        headers: {
          "Device-Trusted": trustDevice.toString(),
        },
      },
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      if (error.response.status === 400) {
        throw new Error(
          error.response.data?.message || "Invalid verification code",
        );
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to verify 2FA");
  }
}

export async function disableTwoFactorAuth(
  code: string,
): Promise<TwoFactorAuthResponse> {
  try {
    const response = await apiClient.delete(
      API_ENDPOINTS.users.twoFactorAuth.disable,
      {
        data: { code },
      },
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        throw new Error("Unauthorized");
      }
      if (error.response.status === 400) {
        throw new Error(
          error.response.data?.message || "Invalid verification code",
        );
      }
      throw new Error(`API error: ${error.response.status}`);
    }
    throw new Error("Failed to disable 2FA");
  }
}
