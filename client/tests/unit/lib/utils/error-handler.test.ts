import {
  AxiosError,
  InternalAxiosRequestConfig,
  AxiosHeaders,
  AxiosResponse,
} from "axios";
import { extractErrorMessage, handleApiError } from "@/lib/utils/error-handler";
import { toast } from "sonner";

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    error: jest.fn(),
  },
}));

// Mock console.error
const mockConsoleError = jest.fn();
const originalConsoleError = console.error;

beforeAll(() => {
  console.error = mockConsoleError;
});

afterAll(() => {
  console.error = originalConsoleError;
});

beforeEach(() => {
  mockConsoleError.mockClear();
  jest.clearAllMocks();
});

// Helper function to create mock Axios config
const createMockAxiosConfig = (): InternalAxiosRequestConfig => ({
  headers: new AxiosHeaders(),
  method: "get",
  url: "",
  data: undefined,
  baseURL: "",
  transformRequest: [],
  transformResponse: [],
  timeout: 0,
  xsrfCookieName: "",
  xsrfHeaderName: "",
  maxContentLength: 0,
  maxBodyLength: 0,
  env: {},
  formSerializer: { indexes: null },
});

// Helper function to create mock Axios response
const createMockAxiosResponse = (data: unknown = {}): AxiosResponse => ({
  data,
  status: 200,
  statusText: "OK",
  headers: {},
  config: createMockAxiosConfig(),
});

describe("Error Handler Utils", () => {
  describe("extractErrorMessage", () => {
    it("should handle null or undefined errors", () => {
      expect(extractErrorMessage(null)).toBe("An unknown error occurred");
      expect(extractErrorMessage(undefined)).toBe("An unknown error occurred");
    });

    it("should handle string errors", () => {
      expect(extractErrorMessage("Test error")).toBe("Test error");
    });

    it("should handle standard Error objects", () => {
      const error = new Error("Test error");
      expect(extractErrorMessage(error)).toBe("Test error");
    });

    it("should handle Axios errors with string response data", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        createMockAxiosResponse("API Error Message"),
      );
      expect(extractErrorMessage(error)).toBe("API Error Message");
    });

    it("should handle Axios errors with error object in response data", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        createMockAxiosResponse({ error: "Validation failed" }),
      );
      expect(extractErrorMessage(error)).toBe("Validation failed");
    });

    it("should handle Axios errors with message object in response data", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        createMockAxiosResponse({ message: "Invalid input" }),
      );
      expect(extractErrorMessage(error)).toBe("Invalid input");
    });

    it("should handle Axios errors with array of errors in response data", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        createMockAxiosResponse({
          errors: [{ message: "Error 1" }, { message: "Error 2" }],
        }),
      );
      expect(extractErrorMessage(error)).toBe("Error 1, Error 2");
    });

    it("should handle Axios errors with no response data but status text", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        {
          ...createMockAxiosResponse(),
          status: 404,
          statusText: "Not Found",
          data: undefined,
        },
      );
      expect(extractErrorMessage(error)).toBe("Not Found (404)");
    });

    it("should handle Axios errors with no response data and no status text", () => {
      const error = new AxiosError(
        "Network Error",
        "ERR_NETWORK",
        createMockAxiosConfig(),
        null,
        {
          ...createMockAxiosResponse(),
          status: 500,
          statusText: "",
          data: undefined,
        },
      );
      expect(extractErrorMessage(error)).toBe("Network Error");
    });

    it("should handle unknown error types", () => {
      expect(extractErrorMessage({})).toBe("An unexpected error occurred");
      expect(extractErrorMessage(123)).toBe("An unexpected error occurred");
    });
  });

  describe("handleApiError", () => {
    it("should log error and show toast when shouldLog is true", () => {
      const error = new Error("Test error");
      handleApiError(error, "Fallback message", true);

      expect(mockConsoleError).toHaveBeenCalledWith("API Error:", error);
      expect(toast.error).toHaveBeenCalledWith("Test error");
    });

    it("should not log error when shouldLog is false", () => {
      const error = new Error("Test error");
      handleApiError(error, "Fallback message", false);

      expect(mockConsoleError).not.toHaveBeenCalled();
      expect(toast.error).toHaveBeenCalledWith("Test error");
    });

    it("should use fallback message when error message cannot be extracted", () => {
      handleApiError(null, "Fallback message", false);
      expect(toast.error).toHaveBeenCalledWith("An unknown error occurred");
    });

    it("should return the error message", () => {
      const result = handleApiError(
        new Error("Test error"),
        "Fallback message",
        false,
      );
      expect(result).toBe("Test error");
    });
  });
});
