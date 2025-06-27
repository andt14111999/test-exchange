import { apiClient } from "@/lib/api/client";
import { loginWithGoogle } from "@/lib/api/auth";
import { API_ENDPOINTS } from "@/lib/api/config";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
  },
}));

describe("Auth API", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("loginWithGoogle", () => {
    it("should call Google login endpoint successfully", async () => {
      const mockResponse = {
        data: {
          auth_url: "https://google.com/auth",
        },
      };

      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await loginWithGoogle();

      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.auth.google);
      expect(result).toEqual(mockResponse.data);
    });

    it("should handle error during Google login", async () => {
      const mockError = new Error("Failed to get Google auth URL");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(loginWithGoogle()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });
});
