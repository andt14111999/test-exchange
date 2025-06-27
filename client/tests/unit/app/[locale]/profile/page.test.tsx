import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import ProfilePage from "@/app/[locale]/profile/page";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";
import { useTranslations } from "next-intl";

interface User {
  id: string;
  email: string;
  name: string | null;
  username?: string | null;
  role: string;
  avatar?: string;
  isMerchant?: boolean;
  status?: string | null;
  kycLevel?: number;
  phoneVerified?: boolean;
  documentVerified?: boolean;
}

// Mock the dependencies
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock API calls to prevent async errors
jest.mock("@/lib/api/banks", () => ({
  getBanks: jest.fn().mockResolvedValue([]),
}));

jest.mock("@/lib/api/bank-accounts", () => ({
  getBankAccounts: jest.fn().mockResolvedValue([]),
}));

jest.mock("@/lib/api/user", () => ({
  getCurrentUser: jest.fn().mockResolvedValue({}),
}));

jest.mock("@/lib/api/balance", () => ({
  getBalance: jest.fn().mockResolvedValue({}),
}));

jest.mock("@/lib/api/notifications", () => ({
  getNotifications: jest.fn().mockResolvedValue([]),
}));

interface ProtectedLayoutProps {
  children: React.ReactNode;
  loadingFallback: React.ReactNode;
}

jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children, loadingFallback }: ProtectedLayoutProps) => (
    <div data-testid="protected-layout">
      {children}
      {loadingFallback}
    </div>
  ),
}));

// Mock Next.js Image component
jest.mock("next/image", () => ({
  __esModule: true,
  default: ({
    src,
    alt,
    width,
    height,
    className,
  }: {
    src: string;
    alt: string;
    width: number;
    height: number;
    className: string;
  }) => (
    <img
      src={src}
      alt={alt}
      width={width}
      height={height}
      className={className}
      data-testid="user-avatar"
    />
  ),
}));

describe("ProfilePage", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  type TranslationKey =
    | "profile.title"
    | "profile.email"
    | "profile.role"
    | "profile.username"
    | "profile.notSet"
    | "profile.kycLevel"
    | "profile.phoneVerified"
    | "profile.documentVerified"
    | "profile.setUsername"
    | "common.roles.user"
    | "common.roles.admin"
    | "common.roles.merchant"
    | "common.yes"
    | "common.no";

  const mockTranslations: Record<TranslationKey, string> = {
    "profile.title": "Profile Information",
    "profile.email": "Email",
    "profile.role": "Role",
    "profile.username": "Username",
    "profile.notSet": "Not set",
    "profile.kycLevel": "KYC Level",
    "profile.phoneVerified": "Phone Verification",
    "profile.documentVerified": "Document Verification",
    "profile.setUsername": "Set Username",
    "common.roles.user": "User",
    "common.roles.admin": "Administrator",
    "common.roles.merchant": "Merchant",
    "common.yes": "Yes",
    "common.no": "No",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useTranslations as jest.Mock).mockReturnValue(
      (key: TranslationKey) => mockTranslations[key],
    );
  });

  describe("Loading State", () => {
    it("renders loading state correctly when user is null", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({ user: null });

      render(<ProfilePage />);

      expect(screen.getByTestId("protected-layout")).toBeInTheDocument();
      expect(screen.getByText("Profile Information")).toBeInTheDocument();
      // Check that skeleton elements are rendered (they have animate-pulse class)
      const skeletonElements = document.querySelectorAll(".animate-pulse");
      expect(skeletonElements.length).toBeGreaterThan(0);
    });

    it("renders loading state correctly when user is undefined", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: undefined,
      });

      render(<ProfilePage />);

      expect(screen.getByTestId("protected-layout")).toBeInTheDocument();
      expect(screen.getByText("Profile Information")).toBeInTheDocument();
    });
  });

  describe("User Profile Rendering", () => {
    it("renders user profile with avatar", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "admin",
        avatar: "https://example.com/avatar.jpg",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("john@example.com")).toBeInTheDocument();
      expect(screen.getByText("Role: Administrator")).toBeInTheDocument();
      expect(screen.getByTestId("user-avatar")).toBeInTheDocument();
    });

    it("renders user profile without avatar", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("john@example.com")).toBeInTheDocument();
      expect(screen.getByText("Role: User")).toBeInTheDocument();
      expect(screen.queryByTestId("user-avatar")).not.toBeInTheDocument();
    });

    it("renders user profile with email as name when name is empty", () => {
      const mockUser: User = {
        id: "1",
        email: "john@example.com",
        role: "user",
        name: "",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const emailInHeader = screen.getByRole("heading", {
        name: "john@example.com",
      });
      expect(emailInHeader).toBeInTheDocument();
      expect(screen.getByText("Role: User")).toBeInTheDocument();
    });

    it("renders user profile with email as name when name is null", () => {
      const mockUser: User = {
        id: "1",
        email: "john@example.com",
        role: "user",
        name: null as string | null,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const emailInHeader = screen.getByRole("heading", {
        name: "john@example.com",
      });
      expect(emailInHeader).toBeInTheDocument();
      expect(screen.getByText("Role: User")).toBeInTheDocument();
    });

    it("renders user profile with avatar when user has avatar", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        avatar: "https://example.com/avatar.jpg",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByTestId("user-avatar")).toBeInTheDocument();
      expect(screen.getByTestId("user-avatar")).toHaveAttribute(
        "src",
        "https://example.com/avatar.jpg",
      );
    });

    it("renders user profile without avatar when avatar is empty string", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        avatar: "", // Test empty string avatar to cover line 101
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.queryByTestId("user-avatar")).not.toBeInTheDocument();
    });
  });

  describe("Status Display", () => {
    it("renders active status with correct styling", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "active",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const statusElement = screen.getByText("active");
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveClass(
        "bg-green-100",
        "text-green-800",
        "border-green-300",
      );
    });

    it("renders suspended status with correct styling", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "suspended",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const statusElement = screen.getByText("suspended");
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveClass(
        "bg-yellow-100",
        "text-yellow-800",
        "border-yellow-300",
      );
    });

    it("renders banned status with correct styling", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "banned",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const statusElement = screen.getByText("banned");
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveClass(
        "bg-red-100",
        "text-red-800",
        "border-red-300",
      );
    });

    it("renders unknown status with default styling", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "unknown",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const statusElement = screen.getByText("unknown");
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveClass(
        "bg-gray-100",
        "text-gray-800",
        "border-gray-300",
      );
    });

    it("handles status with different case", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "ACTIVE",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const statusElement = screen.getByText("ACTIVE");
      expect(statusElement).toBeInTheDocument();
      expect(statusElement).toHaveClass(
        "bg-green-100",
        "text-green-800",
        "border-green-300",
      );
    });

    it("does not render status when status is undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.queryByText("active")).not.toBeInTheDocument();
      expect(screen.queryByText("suspended")).not.toBeInTheDocument();
      expect(screen.queryByText("banned")).not.toBeInTheDocument();
    });

    it("handles getStatusColor with falsy status", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: "", // Empty string to test falsy status
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      // The status should not be rendered when it's falsy
      // Check that no status element is rendered
      expect(
        screen.queryByText(/active|suspended|banned/),
      ).not.toBeInTheDocument();
    });

    it("handles getStatusColor with null status", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: null as string | null, // Test null status to cover line 36
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      // The status should not be rendered when it's null
      expect(
        screen.queryByText(/active|suspended|banned/),
      ).not.toBeInTheDocument();
    });

    it("handles getStatusColor with undefined status", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        status: undefined, // Test undefined status to cover line 36
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      // The status should not be rendered when it's undefined
      expect(
        screen.queryByText(/active|suspended|banned/),
      ).not.toBeInTheDocument();
    });
  });

  describe("Username Display", () => {
    it("renders username when available", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: "johndoe",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Username: johndoe")).toBeInTheDocument();
    });

    it("renders 'Not set' when username is null", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: null,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Username: Not set")).toBeInTheDocument();
    });

    it("renders 'Not set' when username is undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Username: Not set")).toBeInTheDocument();
    });

    it("renders 'Not set' when username is empty string", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: "",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Username: Not set")).toBeInTheDocument();
    });
  });

  describe("Role Display", () => {
    it("renders user role correctly", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Role: User")).toBeInTheDocument();
    });

    it("renders admin role correctly", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "admin",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Role: Administrator")).toBeInTheDocument();
    });

    it("renders merchant role correctly", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "merchant",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Role: Merchant")).toBeInTheDocument();
    });

    it("renders default user role when role is undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
      } as User;

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Role: User")).toBeInTheDocument();
    });

    it("renders default user role when role is empty string", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Role: User")).toBeInTheDocument();
    });
  });

  describe("KYC Level Display", () => {
    it("renders KYC level when available", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        kycLevel: 2,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("KYC Level: 2")).toBeInTheDocument();
    });

    it("renders KYC level 0", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        kycLevel: 0,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("KYC Level: 0")).toBeInTheDocument();
    });

    it("does not render KYC level when undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.queryByText(/KYC Level/)).not.toBeInTheDocument();
    });
  });

  describe("Phone Verification Display", () => {
    it("renders phone verification as verified", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        phoneVerified: true,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Phone Verification: Yes")).toBeInTheDocument();
    });

    it("renders phone verification as not verified", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        phoneVerified: false,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Phone Verification: No")).toBeInTheDocument();
    });

    it("does not render phone verification when undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.queryByText(/Phone Verification/)).not.toBeInTheDocument();
    });
  });

  describe("Document Verification Display", () => {
    it("renders document verification as verified", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        documentVerified: true,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(
        screen.getByText("Document Verification: Yes"),
      ).toBeInTheDocument();
    });

    it("renders document verification as not verified", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        documentVerified: false,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.getByText("Document Verification: No")).toBeInTheDocument();
    });

    it("does not render document verification when undefined", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(
        screen.queryByText(/Document Verification/),
      ).not.toBeInTheDocument();
    });
  });

  describe("Set Username Button", () => {
    it("shows set username button when username is not set", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: null,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const setUsernameButton = screen.getByText("Set Username");
      expect(setUsernameButton).toBeInTheDocument();

      fireEvent.click(setUsernameButton);
      expect(mockRouter.push).toHaveBeenCalledWith("/profile/update");
    });

    it("shows set username button when username is empty string", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: "",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      const setUsernameButton = screen.getByText("Set Username");
      expect(setUsernameButton).toBeInTheDocument();
    });

    it("does not show set username button when username is set", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        role: "user",
        username: "johndoe",
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      expect(screen.queryByText("Set Username")).not.toBeInTheDocument();
    });
  });

  describe("Redirect Logic", () => {
    it("redirects to profile update when user has no username", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: null,
        role: "user",
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useRouter as jest.Mock).mockReturnValue({ push: jest.fn() });

      render(<ProfilePage />);

      expect(useRouter().push).toHaveBeenCalledWith("/profile/update");
    });

    it("redirects to profile update when user has empty username", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "",
        role: "user",
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useRouter as jest.Mock).mockReturnValue({ push: jest.fn() });

      render(<ProfilePage />);

      expect(useRouter().push).toHaveBeenCalledWith("/profile/update");
    });

    it("does not redirect when user has username", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "testuser",
        role: "user",
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useRouter as jest.Mock).mockReturnValue({ push: jest.fn() });

      render(<ProfilePage />);

      expect(useRouter().push).not.toHaveBeenCalled();
    });

    it("does not redirect when user is null", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({ user: null });
      (useRouter as jest.Mock).mockReturnValue({ push: jest.fn() });

      render(<ProfilePage />);

      expect(useRouter().push).not.toHaveBeenCalled();
    });

    it("does not redirect when user is undefined", () => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: undefined,
      });
      (useRouter as jest.Mock).mockReturnValue({ push: jest.fn() });

      render(<ProfilePage />);

      expect(useRouter().push).not.toHaveBeenCalled();
    });
  });

  describe("Avatar Display", () => {
    it("renders avatar image when user has avatar", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "testuser",
        role: "user",
        avatar: "https://example.com/avatar.jpg",
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useTranslations as jest.Mock).mockReturnValue((key: string) => key);

      render(<ProfilePage />);

      const avatarImage = screen.getByTestId("user-avatar");
      expect(avatarImage).toBeInTheDocument();
      expect(avatarImage).toHaveAttribute(
        "src",
        "https://example.com/avatar.jpg",
      );
      expect(avatarImage).toHaveAttribute("alt", "Test User");
    });

    it("renders user icon when user has no avatar", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "testuser",
        role: "user",
        avatar: null,
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useTranslations as jest.Mock).mockReturnValue((key: string) => key);

      render(<ProfilePage />);

      expect(screen.queryByTestId("user-avatar")).not.toBeInTheDocument();
      // Check for SVG user icon by class
      expect(document.querySelector(".lucide-user")).toBeInTheDocument();
    });

    it("renders user icon when user has empty avatar", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "testuser",
        role: "user",
        avatar: "",
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useTranslations as jest.Mock).mockReturnValue((key: string) => key);

      render(<ProfilePage />);

      expect(screen.queryByTestId("user-avatar")).not.toBeInTheDocument();
      expect(document.querySelector(".lucide-user")).toBeInTheDocument();
    });

    it("renders user icon when user has undefined avatar", () => {
      const mockUser = {
        id: "1",
        email: "test@example.com",
        name: "Test User",
        username: "testuser",
        role: "user",
        avatar: undefined,
      };
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });
      (useTranslations as jest.Mock).mockReturnValue((key: string) => key);

      render(<ProfilePage />);

      expect(screen.queryByTestId("user-avatar")).not.toBeInTheDocument();
      expect(document.querySelector(".lucide-user")).toBeInTheDocument();
    });
  });

  describe("Complete User Profile", () => {
    it("renders complete user profile with all fields", () => {
      const mockUser: User = {
        id: "1",
        name: "John Doe",
        email: "john@example.com",
        username: "johndoe",
        role: "admin",
        avatar: "https://example.com/avatar.jpg",
        status: "active",
        kycLevel: 3,
        phoneVerified: true,
        documentVerified: true,
      };

      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: mockUser,
      });

      render(<ProfilePage />);

      // Check all user information is displayed
      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("john@example.com")).toBeInTheDocument();
      expect(screen.getByText("Username: johndoe")).toBeInTheDocument();
      expect(screen.getByText("Role: Administrator")).toBeInTheDocument();
      expect(screen.getByText("active")).toBeInTheDocument();
      expect(screen.getByText("KYC Level: 3")).toBeInTheDocument();
      expect(screen.getByText("Phone Verification: Yes")).toBeInTheDocument();
      expect(
        screen.getByText("Document Verification: Yes"),
      ).toBeInTheDocument();
      expect(screen.getByTestId("user-avatar")).toBeInTheDocument();

      // Check buttons
      expect(screen.queryByText("Set Username")).not.toBeInTheDocument();
    });
  });
});
