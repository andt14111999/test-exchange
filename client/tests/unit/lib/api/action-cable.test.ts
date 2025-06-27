import { createActionCableConsumer } from "@/lib/api/action-cable";
import { WS_URL } from "@/lib/api/config";
import { createConsumer } from "@rails/actioncable";

// Mock @rails/actioncable
jest.mock("@rails/actioncable", () => ({
  createConsumer: jest.fn(),
}));

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

describe("createActionCableConsumer", () => {
  const mockToken = "test-token";
  const mockConsumer = { subscribe: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset localStorage mock
    mockLocalStorage.getItem.mockReturnValue(null);
    // Reset createConsumer mock
    (createConsumer as jest.Mock).mockReturnValue(mockConsumer);
  });

  it("should return null when token is not available", () => {
    mockLocalStorage.getItem.mockReturnValue(null);
    const consumer = createActionCableConsumer();
    expect(consumer).toBeNull();
    expect(mockLocalStorage.getItem).toHaveBeenCalledWith("token");
  });

  it("should create consumer with correct URL when token is available", () => {
    mockLocalStorage.getItem.mockReturnValue(mockToken);
    const consumer = createActionCableConsumer();

    expect(mockLocalStorage.getItem).toHaveBeenCalledWith("token");
    expect(createConsumer).toHaveBeenCalledWith(
      `${WS_URL}/cable?token=${mockToken}`,
    );
    expect(consumer).toBe(mockConsumer);
  });
});
