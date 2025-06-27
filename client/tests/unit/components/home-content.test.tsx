import { render, screen, waitFor } from "@testing-library/react";
import { HomeContent } from "@/components/home-content";
import { useUserStore } from "@/lib/store/user-store";
import { useAuth } from "@/hooks/use-auth";

// Mock the dependencies
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => {
    const translations: Record<string, string> = {
      title: "Welcome to Snowfox",
      welcome: "Welcome to our platform",
    };
    return translations[key] || key;
  }),
}));

// Mock the store with proper types
type UserState = {
  user: {
    name?: string;
    role?: string;
  } | null;
};

jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

jest.mock("@/hooks/use-auth", () => ({
  useAuth: jest.fn(),
}));

describe("HomeContent", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading state correctly", () => {
    // Mock loading state
    (useAuth as jest.Mock).mockReturnValue({ isLoading: true });
    (useUserStore as unknown as jest.Mock<UserState>).mockReturnValue({
      user: null,
    });

    render(<HomeContent />);

    expect(screen.getByText("Welcome to Snowfox")).toBeInTheDocument();
    expect(screen.getByText("Welcome to our platform")).toBeInTheDocument();
  });

  it("renders initial unmounted state correctly", () => {
    // Mock unmounted state
    (useAuth as jest.Mock).mockReturnValue({ isLoading: false });
    (useUserStore as unknown as jest.Mock<UserState>).mockReturnValue({
      user: null,
    });

    render(<HomeContent />);

    expect(screen.getByText("Welcome to Snowfox")).toBeInTheDocument();
    expect(screen.getByText("Welcome to our platform")).toBeInTheDocument();
  });

  it("renders welcome message for anonymous user", async () => {
    // Mock authenticated state with no user
    (useAuth as jest.Mock).mockReturnValue({ isLoading: false });
    (useUserStore as unknown as jest.Mock<UserState>).mockReturnValue({
      user: null,
    });

    render(<HomeContent />);

    // Wait for component to mount
    await waitFor(() => {
      expect(screen.getByText("Welcome to Snowfox")).toBeInTheDocument();
      expect(screen.getByText("Welcome to our platform")).toBeInTheDocument();
    });
  });

  it("renders personalized welcome message for user with name", async () => {
    // Mock authenticated state with user
    (useAuth as jest.Mock).mockReturnValue({ isLoading: false });
    (useUserStore as unknown as jest.Mock<UserState>).mockReturnValue({
      user: {
        name: "John Doe",
        role: "user",
      },
    });

    render(<HomeContent />);

    // Wait for component to mount
    await waitFor(() => {
      expect(screen.getByText("Welcome to Snowfox")).toBeInTheDocument();
      expect(screen.getByText("Welcome back, John Doe!")).toBeInTheDocument();
      expect(screen.getByText("Your role: user")).toBeInTheDocument();
    });
  });

  it("renders personalized welcome message for user without name", async () => {
    // Mock authenticated state with user but no name
    (useAuth as jest.Mock).mockReturnValue({ isLoading: false });
    (useUserStore as unknown as jest.Mock<UserState>).mockReturnValue({
      user: {
        name: "",
        role: "admin",
      },
    });

    render(<HomeContent />);

    // Wait for component to mount
    await waitFor(() => {
      expect(screen.getByText("Welcome to Snowfox")).toBeInTheDocument();
      expect(screen.getByText("Welcome back, User!")).toBeInTheDocument();
      expect(screen.getByText("Your role: admin")).toBeInTheDocument();
    });
  });
});
