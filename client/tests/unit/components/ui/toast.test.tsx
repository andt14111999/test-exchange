import { render, screen, act } from "@testing-library/react";
import { Toast } from "@/components/ui/toast";
import { useToast } from "@/components/ui/use-toast";
import "@testing-library/jest-dom";

// Mock the useToast hook
jest.mock("@/components/ui/use-toast", () => ({
  useToast: jest.fn(),
}));

describe("Toast Component", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it("renders nothing when there are no toasts", () => {
    (useToast as jest.Mock).mockReturnValue({
      toasts: [],
      dismissToast: jest.fn(),
    });

    const { container } = render(<Toast />);
    expect(container.firstChild).toBeNull();
  });

  it("renders a single toast with title only", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Test Toast",
          variant: "default",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);
    const toastContainer = screen.getByTestId("toast-container");
    const toastElement = screen.getByTestId("toast-0");
    expect(toastContainer).toBeInTheDocument();
    expect(toastElement).toBeInTheDocument();
    expect(screen.getByText("Test Toast")).toBeInTheDocument();
    expect(screen.queryByTestId("toast-description-0")).not.toBeInTheDocument();
  });

  it("renders a toast with title and description", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Test Toast",
          description: "Test Description",
          variant: "default",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);
    const toastContainer = screen.getByTestId("toast-container");
    const toastElement = screen.getByTestId("toast-0");
    const toastDescription = screen.getByTestId("toast-description-0");
    expect(toastContainer).toBeInTheDocument();
    expect(toastElement).toBeInTheDocument();
    expect(screen.getByText("Test Toast")).toBeInTheDocument();
    expect(toastDescription).toHaveTextContent("Test Description");
  });

  it("renders multiple toasts", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Toast 1",
          variant: "default",
        },
        {
          title: "Toast 2",
          description: "Description 2",
          variant: "destructive",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);
    const toastContainer = screen.getByTestId("toast-container");
    expect(toastContainer).toBeInTheDocument();
    expect(screen.getByTestId("toast-0")).toBeInTheDocument();
    expect(screen.getByTestId("toast-1")).toBeInTheDocument();
    expect(screen.getByText("Toast 1")).toBeInTheDocument();
    expect(screen.getByText("Toast 2")).toBeInTheDocument();
    expect(screen.getByTestId("toast-description-1")).toHaveTextContent(
      "Description 2",
    );
  });

  it("applies correct styles for default variant", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Default Toast",
          variant: "default",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);
    const toastElement = screen.getByTestId("toast-0");
    expect(toastElement).toHaveClass(
      "bg-white",
      "text-gray-800",
      "border-gray-200",
    );
  });

  it("applies correct styles for destructive variant", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Error Toast",
          variant: "destructive",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);
    const toastElement = screen.getByTestId("toast-0");
    expect(toastElement).toHaveClass(
      "bg-red-50",
      "text-red-800",
      "border-red-200",
    );
  });

  it("calls dismissToast after timeout", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Test Toast",
          variant: "default",
        },
      ],
      dismissToast: mockDismissToast,
    });

    render(<Toast />);

    expect(mockDismissToast).not.toHaveBeenCalled();

    // Fast-forward time by 5 seconds
    act(() => {
      jest.advanceTimersByTime(5000);
    });

    expect(mockDismissToast).toHaveBeenCalledTimes(1);
  });

  it("cleans up timeout on unmount", () => {
    const mockDismissToast = jest.fn();
    (useToast as jest.Mock).mockReturnValue({
      toasts: [
        {
          title: "Test Toast",
          variant: "default",
        },
      ],
      dismissToast: mockDismissToast,
    });

    const { unmount } = render(<Toast />);

    // Create spy on clearTimeout
    const clearTimeoutSpy = jest.spyOn(global, "clearTimeout");

    unmount();

    expect(clearTimeoutSpy).toHaveBeenCalled();
    clearTimeoutSpy.mockRestore();
  });
});
