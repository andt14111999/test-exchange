import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ImageViewer } from "@/components/ui/image-viewer";

// Mock Next.js Image component
jest.mock("next/image", () => {
  return function MockImage(props: Record<string, unknown>) {
    return <img data-testid="next-image" {...props} />;
  };
});

// Mock window.Image
const mockImageLoad = jest.fn();
const mockImageError = jest.fn();

Object.defineProperty(window, "Image", {
  configurable: true,
  value: jest.fn().mockImplementation(() => ({
    naturalWidth: 800,
    naturalHeight: 600,
    set onload(fn: () => void) {
      mockImageLoad.mockImplementation(fn);
    },
    set onerror(fn: () => void) {
      mockImageError.mockImplementation(fn);
    },
    set src(_src: string) {
      // Trigger onload after a short delay to simulate loading
      setTimeout(() => mockImageLoad(), 0);
    },
  })),
});

describe("ImageViewer", () => {
  const defaultProps = {
    src: "https://example.com/test-image.jpg",
    alt: "Test image",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("shows loading skeleton initially", () => {
    render(<ImageViewer {...defaultProps} />);

    expect(screen.getByTestId("loading-skeleton")).toBeInTheDocument();
    expect(screen.getByTestId("loading-skeleton")).toHaveClass(
      "animate-pulse",
      "bg-gray-200",
    );
  });

  it("shows loading skeleton with custom height", () => {
    render(<ImageViewer {...defaultProps} maxHeight={300} />);

    const skeleton = screen.getByTestId("loading-skeleton");
    expect(skeleton).toHaveStyle("height: 300px");
  });

  it("applies custom className to loading skeleton", () => {
    render(<ImageViewer {...defaultProps} className="custom-class" />);

    expect(screen.getByTestId("loading-skeleton")).toHaveClass("custom-class");
  });

  it("displays image after successful load", async () => {
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    expect(screen.getByTestId("next-image")).toHaveAttribute(
      "src",
      defaultProps.src,
    );
    expect(screen.getByTestId("next-image")).toHaveAttribute(
      "alt",
      defaultProps.alt,
    );
  });

  it("shows zoom icon by default", async () => {
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("zoom-icon")).toBeInTheDocument();
    });
  });

  it("hides zoom icon when showZoomIcon is false", async () => {
    render(<ImageViewer {...defaultProps} showZoomIcon={false} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    expect(screen.queryByTestId("zoom-icon")).not.toBeInTheDocument();
  });

  it("opens dialog when image is clicked", async () => {
    const user = userEvent.setup();
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("image-container"));

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByTestId("dialog-image")).toBeInTheDocument();
  });

  it("closes dialog when close button is clicked", async () => {
    const user = userEvent.setup();
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    // Open dialog
    await user.click(screen.getByTestId("image-container"));
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    // Close dialog
    await user.click(screen.getByTestId("close-button"));

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  it("includes visually hidden dialog title", async () => {
    const user = userEvent.setup();
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("image-container"));

    expect(screen.getByText("Image Viewer")).toBeInTheDocument();
  });

  it("applies custom className", async () => {
    render(<ImageViewer {...defaultProps} className="custom-class" />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    expect(screen.getByTestId("image-container")).toHaveClass("custom-class");
  });

  it("displays image with correct attributes in dialog", async () => {
    const user = userEvent.setup();
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("image-container"));

    const dialogImage = screen.getByTestId("dialog-image");
    expect(dialogImage).toHaveAttribute("src", defaultProps.src);
    expect(dialogImage).toHaveAttribute("alt", defaultProps.alt);
    expect(dialogImage).toHaveClass(
      "block",
      "max-w-full",
      "max-h-[90vh]",
      "object-contain",
    );
  });

  it("applies correct styling to dialog container", async () => {
    const user = userEvent.setup();
    render(<ImageViewer {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId("image-container")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("image-container"));

    const dialogContainer = screen.getByTestId("dialog-container");
    expect(dialogContainer).toHaveClass(
      "relative",
      "flex",
      "items-center",
      "justify-center",
      "min-h-0",
    );
  });
});
