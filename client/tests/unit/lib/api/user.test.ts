import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import {
  fetchUserData,
  updateUsername,
  enableTwoFactorAuth,
  verifyTwoFactorAuth,
  disableTwoFactorAuth,
} from "@/lib/api/user";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    patch: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
  },
}));

// Mock axios
jest.mock("axios", () => ({
  isAxiosError: jest.fn(),
}));

import axios from "axios";

const mockedAxios = jest.mocked(axios);

describe("User API", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fetchUserData", () => {
    it("should fetch user data successfully - data format 1", async () => {
      const mockUserData = {
        id: "123",
        email: "test@example.com",
        display_name: "Test User",
        role: "user",
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: { data: mockUserData },
      });

      const result = await fetchUserData();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
      expect(result).toEqual(mockUserData);
    });

    it("should fetch user data successfully - data format 2", async () => {
      const mockUserData = {
        id: "123",
        email: "test@example.com",
        display_name: "Test User",
        role: "user",
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce({
        data: mockUserData,
      });

      const result = await fetchUserData();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
      expect(result).toEqual(mockUserData);
    });

    it("should handle unauthorized error", async () => {
      const axiosError = {
        response: {
          status: 401,
        },
      };

      (apiClient.get as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(fetchUserData()).rejects.toThrow("Unauthorized");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });

    it("should handle other HTTP errors", async () => {
      const axiosError = {
        response: {
          status: 500,
        },
      };

      (apiClient.get as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(fetchUserData()).rejects.toThrow("API error: 500");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });

    it("should handle non-axios errors", async () => {
      const error = new Error("Network error");

      (apiClient.get as jest.Mock).mockRejectedValueOnce(error);
      mockedAxios.isAxiosError.mockReturnValue(false);

      await expect(fetchUserData()).rejects.toThrow("Network error");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });

    it("should handle invalid response format", async () => {
      (apiClient.get as jest.Mock).mockResolvedValueOnce({});

      await expect(fetchUserData()).rejects.toThrow("Invalid response format");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });
  });

  describe("updateUsername", () => {
    const username = "newusername";

    it("should update username successfully - data format 1", async () => {
      const mockResponse = {
        data: {
          data: {
            id: "123",
            email: "test@example.com",
            username: username,
          },
        },
      };

      (apiClient.patch as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await updateUsername(username);

      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
      expect(result).toEqual(mockResponse.data.data);
    });

    it("should update username successfully - data format 2", async () => {
      const mockResponse = {
        data: {
          id: "123",
          email: "test@example.com",
          username: username,
        },
      };

      (apiClient.patch as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await updateUsername(username);

      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("should add username to response if missing", async () => {
      const mockResponse = {
        data: {
          id: "123",
          email: "test@example.com",
        },
      };

      (apiClient.patch as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await updateUsername(username);

      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
      expect(result).toEqual({
        ...mockResponse.data,
        username,
      });
    });

    it("should handle unauthorized error", async () => {
      const axiosError = {
        response: {
          status: 401,
        },
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(updateUsername(username)).rejects.toThrow("Unauthorized");
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle validation errors", async () => {
      const axiosError = {
        response: {
          status: 422,
          data: {
            errors: ["Username has already been taken"],
          },
        },
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(updateUsername(username)).rejects.toThrow(
        "Username has already been taken",
      );
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle other HTTP errors", async () => {
      const axiosError = {
        response: {
          status: 500,
        },
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(updateUsername(username)).rejects.toThrow("API error: 500");
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle non-axios errors", async () => {
      const error = new Error("Network error");

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(error);
      mockedAxios.isAxiosError.mockReturnValue(false);

      await expect(updateUsername(username)).rejects.toThrow("Network error");
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle invalid response format", async () => {
      (apiClient.patch as jest.Mock).mockResolvedValueOnce({ data: null });

      await expect(updateUsername(username)).rejects.toThrow(
        "Invalid response format",
      );
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });
  });

  describe("enableTwoFactorAuth", () => {
    it("successfully generates QR code for 2FA setup", async () => {
      const mockResponse = {
        data: {
          qr_code_uri: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
          message: "2FA setup initiated",
        },
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await enableTwoFactorAuth();

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.users.twoFactorAuth.enable,
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 401 unauthorized error", async () => {
      const axiosError = {
        response: { status: 401 },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(enableTwoFactorAuth()).rejects.toThrow("Unauthorized");
    });

    it("handles 400 bad request error", async () => {
      const axiosError = {
        response: {
          status: 400,
          data: { message: "2FA is already enabled" },
        },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(enableTwoFactorAuth()).rejects.toThrow(
        "2FA is already enabled",
      );
    });

    it("handles 500 server error", async () => {
      const axiosError = {
        response: { status: 500 },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(enableTwoFactorAuth()).rejects.toThrow("API error: 500");
    });

    it("handles network errors", async () => {
      const error = new Error("Network error");

      (apiClient.post as jest.Mock).mockRejectedValueOnce(error);
      mockedAxios.isAxiosError.mockReturnValue(false);

      await expect(enableTwoFactorAuth()).rejects.toThrow(
        "Failed to enable 2FA",
      );
    });
  });

  describe("verifyTwoFactorAuth", () => {
    const code = "123456";

    it("successfully verifies 2FA code without device trust", async () => {
      const mockResponse = {
        data: { message: "2FA enabled successfully" },
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await verifyTwoFactorAuth(code, false);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.users.twoFactorAuth.verify,
        { code },
        {
          headers: { "Device-Trusted": "false" },
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("successfully verifies 2FA code with device trust", async () => {
      const mockResponse = {
        data: { message: "2FA enabled successfully" },
      };

      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await verifyTwoFactorAuth(code, true);

      expect(apiClient.post).toHaveBeenCalledWith(
        API_ENDPOINTS.users.twoFactorAuth.verify,
        { code },
        {
          headers: { "Device-Trusted": "true" },
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 400 invalid code error", async () => {
      const axiosError = {
        response: {
          status: 400,
          data: { message: "Invalid verification code" },
        },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(verifyTwoFactorAuth(code)).rejects.toThrow(
        "Invalid verification code",
      );
    });

    it("handles 401 unauthorized error", async () => {
      const axiosError = {
        response: { status: 401 },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(verifyTwoFactorAuth(code)).rejects.toThrow("Unauthorized");
    });

    it("handles 500 server error", async () => {
      const axiosError = {
        response: { status: 500 },
      };

      (apiClient.post as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(verifyTwoFactorAuth(code)).rejects.toThrow("API error: 500");
    });

    it("handles network errors", async () => {
      const error = new Error("Network error");

      (apiClient.post as jest.Mock).mockRejectedValueOnce(error);
      mockedAxios.isAxiosError.mockReturnValue(false);

      await expect(verifyTwoFactorAuth(code)).rejects.toThrow(
        "Failed to verify 2FA",
      );
    });
  });

  describe("disableTwoFactorAuth", () => {
    const code = "123456";

    it("successfully disables 2FA", async () => {
      const mockResponse = {
        data: { message: "2FA disabled successfully" },
      };

      (apiClient.delete as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await disableTwoFactorAuth(code);

      expect(apiClient.delete).toHaveBeenCalledWith(
        API_ENDPOINTS.users.twoFactorAuth.disable,
        {
          data: { code },
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 400 invalid code error", async () => {
      const axiosError = {
        response: {
          status: 400,
          data: { message: "Invalid verification code" },
        },
      };

      (apiClient.delete as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(disableTwoFactorAuth(code)).rejects.toThrow(
        "Invalid verification code",
      );
    });

    it("handles 401 unauthorized error", async () => {
      const axiosError = {
        response: { status: 401 },
      };

      (apiClient.delete as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(disableTwoFactorAuth(code)).rejects.toThrow("Unauthorized");
    });

    it("handles 500 server error", async () => {
      const axiosError = {
        response: { status: 500 },
      };

      (apiClient.delete as jest.Mock).mockRejectedValueOnce(axiosError);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(disableTwoFactorAuth(code)).rejects.toThrow(
        "API error: 500",
      );
    });

    it("handles network errors", async () => {
      const error = new Error("Network error");

      (apiClient.delete as jest.Mock).mockRejectedValueOnce(error);
      mockedAxios.isAxiosError.mockReturnValue(false);

      await expect(disableTwoFactorAuth(code)).rejects.toThrow(
        "Failed to disable 2FA",
      );
    });
  });
});
