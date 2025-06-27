import { apiClient } from "@/lib/api/client";
import { API_ENDPOINTS } from "@/lib/api/config";
import { fetchUserData, updateUsername } from "@/lib/api/user";

// Mock trực tiếp cho axios
jest.mock("axios", () => ({
  isAxiosError: jest.fn(),
}));

jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    patch: jest.fn(),
  },
}));

describe("User API", () => {
  const mockIsAxiosError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Gán mockIsAxiosError cho axios.isAxiosError sau khi jest.clearAllMocks()
    jest.requireMock("axios").isAxiosError = mockIsAxiosError;
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
        isAxiosError: true,
      };

      (apiClient.get as jest.Mock).mockRejectedValueOnce(axiosError);
      mockIsAxiosError.mockReturnValueOnce(true);

      await expect(fetchUserData()).rejects.toThrow("Unauthorized");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });

    it("should handle other HTTP errors", async () => {
      const axiosError = {
        response: {
          status: 500,
        },
        isAxiosError: true,
      };

      (apiClient.get as jest.Mock).mockRejectedValueOnce(axiosError);
      mockIsAxiosError.mockReturnValueOnce(true);

      await expect(fetchUserData()).rejects.toThrow("API error: 500");
      expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.users.me);
    });

    it("should handle non-axios errors", async () => {
      const error = new Error("Network error");

      (apiClient.get as jest.Mock).mockRejectedValueOnce(error);
      mockIsAxiosError.mockReturnValueOnce(false);

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
          // username missing in response
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
        username, // Should be added by the function
      });
    });

    it("should handle unauthorized error", async () => {
      const axiosError = {
        response: {
          status: 401,
        },
        isAxiosError: true,
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockIsAxiosError.mockReturnValueOnce(true);

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
            errors: ["Username already taken", "Username must be unique"],
          },
        },
        isAxiosError: true,
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockIsAxiosError.mockReturnValueOnce(true);

      await expect(updateUsername(username)).rejects.toThrow(
        "Username already taken, Username must be unique",
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
        isAxiosError: true,
      };

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(axiosError);
      mockIsAxiosError.mockReturnValueOnce(true);

      await expect(updateUsername(username)).rejects.toThrow("API error: 500");
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle non-axios errors", async () => {
      const error = new Error("Network error");

      (apiClient.patch as jest.Mock).mockRejectedValueOnce(error);
      mockIsAxiosError.mockReturnValueOnce(false);

      await expect(updateUsername(username)).rejects.toThrow("Network error");
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });

    it("should handle invalid response format", async () => {
      (apiClient.patch as jest.Mock).mockResolvedValueOnce({});

      await expect(updateUsername(username)).rejects.toThrow(
        "Invalid response format",
      );
      expect(apiClient.patch).toHaveBeenCalledWith(
        API_ENDPOINTS.users.updateUsername,
        { username },
      );
    });
  });
});
