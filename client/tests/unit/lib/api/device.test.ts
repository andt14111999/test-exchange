import {
  getCurrentDevice,
  getAllDevices,
  trustCurrentDevice,
  removeDevice,
} from "@/lib/api/device";
import { apiClient } from "@/lib/api/client";
import { getDeviceHeaders } from "@/lib/utils/device";
import axios from "axios";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    delete: jest.fn(),
  },
}));

// Mock device utils
jest.mock("@/lib/utils/device", () => ({
  getDeviceHeaders: jest.fn(),
}));

// Mock axios
jest.mock("axios", () => ({
  isAxiosError: jest.fn(),
}));

const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;
const mockGetDeviceHeaders = getDeviceHeaders as jest.MockedFunction<
  typeof getDeviceHeaders
>;
const mockIsAxiosError = axios.isAxiosError as jest.MockedFunction<
  typeof axios.isAxiosError
>;

describe("Device API Functions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getCurrentDevice", () => {
    it("successfully retrieves current device info", async () => {
      const mockResponse = {
        data: {
          id: 1,
          device_type: "web",
          trusted: true,
          first_device: false,
          ip_address: "127.0.0.1",
          location: "Test Location",
          display_name: "Test Device",
          created_at: "2023-01-01T00:00:00Z",
        },
      };

      const mockHeaders = { "Device-ID": "test-device-id" };
      mockGetDeviceHeaders.mockReturnValue(mockHeaders);
      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await getCurrentDevice();

      expect(mockApiClient.get).toHaveBeenCalledWith(
        "/access_devices/current",
        {
          headers: mockHeaders,
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 401 unauthorized error", async () => {
      const mockError = {
        response: { status: 401 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.get.mockRejectedValue(mockError);

      await expect(getCurrentDevice()).rejects.toThrow("Unauthorized");
    });

    it("handles other API errors", async () => {
      const mockError = {
        response: { status: 500 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.get.mockRejectedValue(mockError);

      await expect(getCurrentDevice()).rejects.toThrow("API error: 500");
    });

    it("handles network errors", async () => {
      mockIsAxiosError.mockReturnValue(false);
      mockApiClient.get.mockRejectedValue(new Error("Network error"));

      await expect(getCurrentDevice()).rejects.toThrow(
        "Failed to get current device",
      );
    });
  });

  describe("getAllDevices", () => {
    it("successfully retrieves all devices", async () => {
      const mockDevices = [
        {
          id: 1,
          device_type: "web",
          trusted: true,
          first_device: false,
          ip_address: "127.0.0.1",
          location: "Test Location",
          display_name: "Test Device 1",
          created_at: "2023-01-01T00:00:00Z",
        },
        {
          id: 2,
          device_type: "mobile",
          trusted: false,
          first_device: false,
          ip_address: "192.168.1.1",
          location: "Mobile Location",
          display_name: "Test Device 2",
          created_at: "2023-01-02T00:00:00Z",
        },
      ];

      mockApiClient.get.mockResolvedValue({ data: mockDevices });

      const result = await getAllDevices();

      expect(mockApiClient.get).toHaveBeenCalledWith("/access_devices");
      expect(result).toEqual(mockDevices);
    });

    it("handles 401 unauthorized error", async () => {
      const mockError = {
        response: { status: 401 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.get.mockRejectedValue(mockError);

      await expect(getAllDevices()).rejects.toThrow("Unauthorized");
    });

    it("handles other API errors", async () => {
      const mockError = {
        response: { status: 500 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.get.mockRejectedValue(mockError);

      await expect(getAllDevices()).rejects.toThrow("API error: 500");
    });

    it("handles network errors", async () => {
      mockIsAxiosError.mockReturnValue(false);
      mockApiClient.get.mockRejectedValue(new Error("Network error"));

      await expect(getAllDevices()).rejects.toThrow(
        "Failed to get trusted devices",
      );
    });
  });

  describe("trustCurrentDevice", () => {
    it("successfully trusts current device", async () => {
      const mockResponse = {
        data: {
          id: 1,
          device_type: "web",
          trusted: true,
          first_device: false,
          ip_address: "127.0.0.1",
          location: "Test Location",
          display_name: "Test Device",
          created_at: "2023-01-01T00:00:00Z",
        },
      };

      const mockHeaders = { "Device-Trusted": "true" };
      mockGetDeviceHeaders.mockReturnValue(mockHeaders);
      mockApiClient.get.mockResolvedValue(mockResponse);

      const result = await trustCurrentDevice();

      expect(mockApiClient.get).toHaveBeenCalledWith(
        "/access_devices/current",
        {
          headers: mockHeaders,
        },
      );
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 401 unauthorized error", async () => {
      const mockError = {
        response: { status: 401 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.get.mockRejectedValue(mockError);

      await expect(trustCurrentDevice()).rejects.toThrow("Unauthorized");
    });
  });

  describe("removeDevice", () => {
    it("successfully removes a device", async () => {
      const mockResponse = {
        data: { message: "Device removed successfully" },
      };

      mockApiClient.delete.mockResolvedValue(mockResponse);

      const result = await removeDevice(1);

      expect(mockApiClient.delete).toHaveBeenCalledWith("/access_devices/1");
      expect(result).toEqual(mockResponse.data);
    });

    it("handles 401 unauthorized error", async () => {
      const mockError = {
        response: { status: 401 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.delete.mockRejectedValue(mockError);

      await expect(removeDevice(1)).rejects.toThrow("Unauthorized");
    });

    it("handles 400 bad request error", async () => {
      const mockError = {
        response: {
          status: 400,
          data: { message: "Cannot remove primary device" },
        },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.delete.mockRejectedValue(mockError);

      await expect(removeDevice(1)).rejects.toThrow(
        "Cannot remove primary device",
      );
    });

    it("handles 400 error without message", async () => {
      const mockError = {
        response: { status: 400, data: {} },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.delete.mockRejectedValue(mockError);

      await expect(removeDevice(1)).rejects.toThrow("Cannot remove device");
    });

    it("handles other API errors", async () => {
      const mockError = {
        response: { status: 500 },
      };
      mockIsAxiosError.mockReturnValue(true);
      mockApiClient.delete.mockRejectedValue(mockError);

      await expect(removeDevice(1)).rejects.toThrow("API error: 500");
    });

    it("handles network errors", async () => {
      mockIsAxiosError.mockReturnValue(false);
      mockApiClient.delete.mockRejectedValue(new Error("Network error"));

      await expect(removeDevice(1)).rejects.toThrow("Failed to remove device");
    });
  });
});
