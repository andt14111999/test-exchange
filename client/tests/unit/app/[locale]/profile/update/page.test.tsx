import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import UpdateUsernamePage from "@/app/[locale]/profile/update/page";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";
import { updateUsername } from "@/lib/api/user";
import { toast } from "sonner";

// Mock các dependencies
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("@/lib/api/user", () => ({
  updateUsername: jest.fn(),
}));

jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

describe("UpdateUsernamePage", () => {
  const mockRouter = { push: jest.fn() };
  const mockUpdateUserInStore = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { email: "test@example.com" },
      updateUsername: mockUpdateUserInStore,
    });
  });

  it("redirects to profile page if user already has username", () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { email: "test@example.com", username: "existinguser" },
      updateUsername: mockUpdateUserInStore,
    });

    render(<UpdateUsernamePage />);

    expect(mockRouter.push).toHaveBeenCalledWith("/profile");
  });

  it("renders the update username form", () => {
    render(<UpdateUsernamePage />);

    expect(screen.getByText("profile.updateUsername")).toBeInTheDocument();
    expect(screen.getByText("profile.username")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("profile.usernamePlaceholder"),
    ).toBeInTheDocument();
    expect(screen.getByText("common.cancel")).toBeInTheDocument();
    expect(screen.getByText("profile.saveUsername")).toBeInTheDocument();
  });

  it("navigates back to profile on cancel button click", () => {
    render(<UpdateUsernamePage />);

    const cancelButton = screen.getByText("common.cancel");
    fireEvent.click(cancelButton);

    expect(mockRouter.push).toHaveBeenCalledWith("/profile");
  });

  it("validates username input", async () => {
    render(<UpdateUsernamePage />);

    const usernameInput = screen.getByPlaceholderText(
      "profile.usernamePlaceholder",
    );
    const submitButton = screen.getByText("profile.saveUsername");

    // Test too short username
    fireEvent.change(usernameInput, { target: { value: "ab" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText("Username must be at least 3 characters"),
      ).toBeInTheDocument();
    });

    // Test invalid characters
    fireEvent.change(usernameInput, { target: { value: "user@name" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Username can only contain letters, numbers, and underscores",
        ),
      ).toBeInTheDocument();
    });
  });

  it("submits the form and updates username successfully", async () => {
    (updateUsername as jest.Mock).mockResolvedValue({
      username: "newusername",
    });

    render(<UpdateUsernamePage />);

    const usernameInput = screen.getByPlaceholderText(
      "profile.usernamePlaceholder",
    );
    const submitButton = screen.getByText("profile.saveUsername");

    fireEvent.change(usernameInput, { target: { value: "newusername" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(updateUsername).toHaveBeenCalledWith("newusername");
      expect(mockUpdateUserInStore).toHaveBeenCalledWith("newusername");
      expect(toast.success).toHaveBeenCalledWith("profile.usernameUpdated");
      expect(mockRouter.push).toHaveBeenCalledWith("/profile");
    });
  });

  it("handles API error during username update", async () => {
    const error = new Error("Username already taken");
    (updateUsername as jest.Mock).mockRejectedValue(error);

    render(<UpdateUsernamePage />);

    const usernameInput = screen.getByPlaceholderText(
      "profile.usernamePlaceholder",
    );
    const submitButton = screen.getByText("profile.saveUsername");

    fireEvent.change(usernameInput, { target: { value: "takenusername" } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(updateUsername).toHaveBeenCalledWith("takenusername");
      expect(toast.error).toHaveBeenCalledWith("Username already taken");
      expect(mockRouter.push).not.toHaveBeenCalled();
    });
  });
});
