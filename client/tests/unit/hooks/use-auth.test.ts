import { renderHook, act } from "@testing-library/react";
import { useAuth } from "../../../src/hooks/use-auth";
import { fetchUserData } from "@/lib/api/user";
import { useRouter } from "@/navigation";
import { UserData } from "@/types/user";

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

// Mock next-intl and its dependencies
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

jest.mock("next-intl/server", () => ({
  getRequestConfig: jest.fn(),
  getMessages: jest.fn(),
  getLocale: jest.fn(),
  getTimeZone: jest.fn(),
}));

jest.mock("next-intl/navigation", () => ({
  createNavigation: () => ({
    Link: jest.fn(),
    redirect: jest.fn(),
    usePathname: jest.fn(),
    useRouter: jest.fn(),
  }),
}));

jest.mock("@/config/i18n", () => ({
  locales: ["en", "vi", "fil"],
  defaultLocale: "en",
}));

// Mock other dependencies
jest.mock("@/lib/api/user");
jest.mock("@/navigation");
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: () => ({
    setUser: mockSetUser,
  }),
}));

const mockFetchUserData = fetchUserData as jest.MockedFunction<
  typeof fetchUserData
>;
const mockPush = jest.fn();
const mockSetUser = jest.fn();

// Mock router
(useRouter as jest.Mock).mockReturnValue({
  push: mockPush,
});

describe("useAuth", () => {
  const mockUserData: UserData = {
    id: 123,
    email: "test@example.com",
    display_name: "Test User",
    role: "user",
    avatar_url: "https://example.com/avatar.jpg",
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
  };

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    mockLocalStorage.clear();
    mockLocalStorage.getItem.mockReturnValue(null);
    // Reset window.location.pathname
    Object.defineProperty(window, "location", {
      value: { pathname: "/" },
      writable: true,
    });
  });

  it("should handle loading state correctly", async () => {
    mockLocalStorage.getItem.mockReturnValue(null);

    const { result } = renderHook(() => useAuth());

    // Initial state
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);

    // Wait for effect to complete
    await act(async () => {
      await Promise.resolve();
    });

    // After effect
    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
  });

  it("should handle no token case", async () => {
    mockLocalStorage.getItem.mockReturnValue(null);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockFetchUserData).not.toHaveBeenCalled();
  });

  it("should load user data successfully when token exists", async () => {
    mockLocalStorage.getItem.mockReturnValue("valid-token");
    mockFetchUserData.mockResolvedValueOnce(mockUserData);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user).toEqual(mockUserData);
    expect(mockSetUser).toHaveBeenCalledWith({
      id: mockUserData.id.toString(),
      email: mockUserData.email,
      name: mockUserData.display_name || mockUserData.email,
      role: mockUserData.role,
      avatar: mockUserData.avatar_url,
    });
  });

  it("should handle unauthorized error", async () => {
    mockLocalStorage.getItem.mockReturnValue("invalid-token");
    mockFetchUserData.mockRejectedValueOnce(new Error("Unauthorized"));

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockLocalStorage.removeItem).toHaveBeenCalledWith("token");
    expect(mockPush).toHaveBeenCalledWith("/login");
  });

  it("should not redirect on unauthorized error if already on login page", async () => {
    mockLocalStorage.getItem.mockReturnValue("invalid-token");
    mockFetchUserData.mockRejectedValueOnce(new Error("Unauthorized"));

    // Mock being on login page
    Object.defineProperty(window, "location", {
      value: { pathname: "/login" },
      writable: true,
    });

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockLocalStorage.removeItem).toHaveBeenCalledWith("token");
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("should handle generic error without removing token", async () => {
    mockLocalStorage.getItem.mockReturnValue("valid-token");
    mockFetchUserData.mockRejectedValueOnce(new Error("Network error"));

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockLocalStorage.getItem("token")).toBe("valid-token");
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("should use email as name when display_name is not available", async () => {
    mockLocalStorage.getItem.mockReturnValue("valid-token");
    const userDataWithoutDisplayName: UserData = {
      ...mockUserData,
      display_name: undefined,
    };
    mockFetchUserData.mockResolvedValueOnce(userDataWithoutDisplayName);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user).toEqual(userDataWithoutDisplayName);
    expect(mockSetUser).toHaveBeenCalledWith({
      id: mockUserData.id.toString(),
      email: mockUserData.email,
      name: mockUserData.email,
      role: mockUserData.role,
      avatar: mockUserData.avatar_url,
    });
  });
});
