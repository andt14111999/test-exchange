import { render, screen, fireEvent } from "@testing-library/react";
import { UserMenu } from "@/components/user-menu";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";

// Define the User type from user-store
interface User {
  id: string;
  email: string;
  name: string;
  role: string;
  avatar?: string;
  isMerchant?: boolean;
}

// Mock the dependencies
jest.mock("@/navigation");

// Mock next-intl/navigation
jest.mock("next-intl/navigation", () => ({
  createNavigation: jest.fn(() => ({
    Link: "mocked-link",
    redirect: jest.fn(),
    usePathname: jest.fn(),
    useRouter: jest.fn(),
  })),
}));

// Mock next-intl/server
jest.mock("next-intl/server", () => ({
  getFormatter: jest.fn(),
  getLocale: jest.fn(),
  getMessages: jest.fn(),
  getNow: jest.fn(),
  getRequestConfig: jest.fn(),
  getTimeZone: jest.fn(),
  getTranslations: jest.fn(),
  setRequestLocale: jest.fn(),
}));

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => `translated.${key}`,
}));

// Mock Radix UI DropdownMenu
jest.mock("@/components/ui/dropdown-menu", () => ({
  DropdownMenu: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dropdown-menu">{children}</div>
  ),
  DropdownMenuTrigger: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dropdown-menu-trigger">{children}</div>
  ),
  DropdownMenuContent: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dropdown-menu-content">{children}</div>
  ),
  DropdownMenuItem: ({
    children,
    onClick,
  }: {
    children: React.ReactNode;
    onClick?: () => void;
  }) => (
    <button data-testid="dropdown-menu-item" onClick={onClick}>
      {children}
    </button>
  ),
  DropdownMenuSeparator: () => <hr data-testid="dropdown-menu-separator" />,
}));

// Mock useUserStore with proper types
jest.mock("@/lib/store/user-store");
const mockUseUserStore = useUserStore as unknown as jest.Mock;

describe("UserMenu", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);

    // Mock localStorage
    const localStorageMock = {
      getItem: jest.fn(),
      setItem: jest.fn(),
      removeItem: jest.fn(),
    };
    Object.defineProperty(window, "localStorage", {
      value: localStorageMock,
      writable: true,
    });
  });

  it("renders login button when user is not authenticated", () => {
    mockUseUserStore.mockReturnValue({
      user: null,
    });

    render(<UserMenu />);
    const loginButton = screen.getByTestId("login-icon");
    expect(loginButton).toBeInTheDocument();
  });

  it("renders user menu when user is authenticated", async () => {
    const mockUser: User = {
      id: "1",
      email: "test@example.com",
      name: "Test User",
      role: "user",
    };

    mockUseUserStore.mockReturnValue({
      user: mockUser,
    });

    render(<UserMenu />);

    // Get the user menu button and click it to open the dropdown
    const userMenuButton = screen.getByTestId("user-icon");
    expect(userMenuButton).toBeInTheDocument();
    fireEvent.click(userMenuButton);

    // Now check if dropdown menu items are rendered
    const menuItems = screen.getAllByTestId("dropdown-menu-item");
    expect(menuItems[0]).toHaveTextContent("translated.common.profile");
    expect(menuItems[1]).toHaveTextContent("translated.common.logout");
  });

  it("navigates to profile page when clicking profile menu item", async () => {
    const mockUser: User = {
      id: "1",
      email: "test@example.com",
      name: "Test User",
      role: "user",
    };

    mockUseUserStore.mockReturnValue({
      user: mockUser,
    });

    render(<UserMenu />);

    // Open the dropdown menu
    const userMenuButton = screen.getByTestId("user-icon");
    fireEvent.click(userMenuButton);

    // Click profile menu item
    const menuItems = screen.getAllByTestId("dropdown-menu-item");
    fireEvent.click(menuItems[0]); // Profile is the first item
    expect(mockRouter.push).toHaveBeenCalledWith("/profile");
  });

  it("handles logout correctly", async () => {
    const mockUser: User = {
      id: "1",
      email: "test@example.com",
      name: "Test User",
      role: "user",
    };

    const mockLogout = jest.fn();
    mockUseUserStore.mockReturnValue({
      user: mockUser,
      logout: mockLogout,
    });

    render(<UserMenu />);

    // Open the dropdown menu
    const userMenuButton = screen.getByTestId("user-icon");
    fireEvent.click(userMenuButton);

    // Click logout menu item
    const menuItems = screen.getAllByTestId("dropdown-menu-item");
    fireEvent.click(menuItems[1]); // Logout is the second item

    // Check if localStorage token is removed
    expect(window.localStorage.removeItem).toHaveBeenCalledWith("token");
    expect(mockLogout).toHaveBeenCalled();
    expect(mockRouter.push).toHaveBeenCalledWith("/login");
  });
});
