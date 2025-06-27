import { renderHook, act } from "@testing-library/react";
import { useProtectedRoute } from "@/hooks/use-protected-route";
import { useRouter } from "@/navigation";
import { useUserStore } from "@/lib/store/user-store";
import { useAuth } from "@/hooks/use-auth";
import { Mock } from "jest-mock";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import React from "react";

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

// Mock dependencies
jest.mock("@/navigation");
jest.mock("@/lib/store/user-store");
jest.mock("@/hooks/use-auth");

const mockPush = jest.fn();
const mockUser = {
  id: "1",
  email: "test@example.com",
  name: "Test User",
  role: "user",
};

// Mock router
(useRouter as unknown as Mock).mockReturnValue({
  push: mockPush,
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

function Wrapper({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe("useProtectedRoute", () => {
  let windowSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    windowSpy = jest.spyOn(global, "window", "get");
  });

  afterEach(() => {
    windowSpy.mockRestore();
  });

  it("should handle authenticated user correctly", async () => {
    // Mock window.location
    windowSpy.mockImplementation(() => ({
      location: { pathname: "/" },
    }));

    (useUserStore as unknown as Mock).mockReturnValue({ user: mockUser });
    (useAuth as unknown as Mock).mockReturnValue({ isLoading: false });

    const { result } = renderHook(() => useProtectedRoute(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.user).toBe(mockUser);
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("should redirect to login when no user and not loading", async () => {
    // Mock window.location
    windowSpy.mockImplementation(() => ({
      location: { pathname: "/" },
    }));

    (useUserStore as unknown as Mock).mockReturnValue({ user: null });
    (useAuth as unknown as Mock).mockReturnValue({ isLoading: false });

    const { result } = renderHook(() => useProtectedRoute(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockPush).toHaveBeenCalledWith("/login");
  });

  it("should not redirect to login when already on login page", async () => {
    // Mock window.location for login page
    windowSpy.mockImplementation(() => ({
      location: { pathname: "/login" },
    }));

    (useUserStore as unknown as Mock).mockReturnValue({ user: null });
    (useAuth as unknown as Mock).mockReturnValue({ isLoading: false });

    const { result } = renderHook(() => useProtectedRoute(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.user).toBe(null);
    expect(mockPush).not.toHaveBeenCalled();
  });

  it("should handle loading state correctly", async () => {
    // Mock window.location
    windowSpy.mockImplementation(() => ({
      location: { pathname: "/" },
    }));

    (useUserStore as unknown as Mock).mockReturnValue({ user: null });
    (useAuth as unknown as Mock).mockReturnValue({ isLoading: true });

    const { result } = renderHook(() => useProtectedRoute(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await Promise.resolve();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isLoading).toBe(true);
    expect(result.current.user).toBe(null);
    expect(mockPush).not.toHaveBeenCalled();
  });
});
