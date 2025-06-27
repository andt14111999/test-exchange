import axios, { InternalAxiosRequestConfig, AxiosResponse } from "axios";
import { API_BASE_URL } from "@/lib/api/config";

// Mock axios
const mockRequestInterceptor = jest.fn(
  (config: InternalAxiosRequestConfig) => config,
);
const mockResponseSuccessInterceptor = jest.fn(
  (response: AxiosResponse) => response,
);
const mockResponseErrorInterceptor = jest.fn((error: unknown) =>
  Promise.reject(error),
);

const mockAxiosInstance = {
  interceptors: {
    request: {
      use: jest.fn((interceptor) => {
        mockRequestInterceptor.mockImplementation(interceptor);
        return 1;
      }),
    },
    response: {
      use: jest.fn((success, error) => {
        mockResponseSuccessInterceptor.mockImplementation(success);
        mockResponseErrorInterceptor.mockImplementation(error);
        return 1;
      }),
    },
  },
};

jest.mock("axios", () => ({
  create: jest.fn(() => mockAxiosInstance),
}));

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  removeItem: jest.fn(),
  setItem: jest.fn(),
  clear: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

describe("API Client", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.isolateModules(async () => {
      // Re-initialize the API client to trigger interceptor setup
      await import("@/lib/api/client");
    });
  });

  describe("Configuration", () => {
    it("should create axios instance with correct config", () => {
      expect(axios.create).toHaveBeenCalledWith({
        baseURL: API_BASE_URL,
        headers: {
          "Content-Type": "application/json",
        },
      });
    });
  });

  describe("Request Interceptor", () => {
    it("should add authorization header when token exists", () => {
      const token = "test-token";
      mockLocalStorage.getItem.mockReturnValue(token);

      const config = { headers: {} } as InternalAxiosRequestConfig;
      const result = mockRequestInterceptor(config);

      expect(result.headers.Authorization).toBe(`Bearer ${token}`);
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith("token");
    });

    it("should not add authorization header when token does not exist", () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      const config = { headers: {} } as InternalAxiosRequestConfig;
      const result = mockRequestInterceptor(config);

      expect(result.headers.Authorization).toBeUndefined();
      expect(mockLocalStorage.getItem).toHaveBeenCalledWith("token");
    });
  });

  describe("Response Interceptor", () => {
    it("should pass through successful responses", () => {
      const response = { data: "test" } as AxiosResponse;
      const result = mockResponseSuccessInterceptor(response);

      expect(result).toBe(response);
    });

    it("should handle 401 unauthorized errors", async () => {
      const error = {
        response: {
          status: 401,
          data: "Unauthorized",
        },
      };

      const consoleSpy = jest.spyOn(console, "log");

      await expect(mockResponseErrorInterceptor(error)).rejects.toBe(error);

      expect(consoleSpy).toHaveBeenCalledWith(
        "API Error:",
        401,
        "Unauthorized",
      );
      expect(consoleSpy).toHaveBeenCalledWith(
        "Unauthorized error in API interceptor",
      );
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith("token");
    });

    it("should handle other errors", async () => {
      const error = {
        response: {
          status: 500,
          data: "Server Error",
        },
      };

      const consoleSpy = jest.spyOn(console, "log");

      await expect(mockResponseErrorInterceptor(error)).rejects.toBe(error);

      expect(consoleSpy).toHaveBeenCalledWith(
        "API Error:",
        500,
        "Server Error",
      );
      expect(mockLocalStorage.removeItem).not.toHaveBeenCalled();
    });

    it("should handle errors without response", async () => {
      const error = new Error("Network Error");
      const consoleSpy = jest.spyOn(console, "log");

      await expect(mockResponseErrorInterceptor(error)).rejects.toBe(error);

      expect(consoleSpy).toHaveBeenCalledWith(
        "API Error:",
        undefined,
        undefined,
      );
      expect(mockLocalStorage.removeItem).not.toHaveBeenCalled();
    });
  });
});
