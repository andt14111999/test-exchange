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
