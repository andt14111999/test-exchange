import React from "react";
import RootLayout, { metadata } from "@/app/layout";
import { headers } from "next/headers";

// Mock the next/headers module
jest.mock("next/headers", () => ({
  headers: jest.fn(),
}));

// Mock for the Inter font
jest.mock("next/font/google", () => ({
  Inter: jest.fn().mockImplementation(() => ({
    className: "className-mock",
  })),
}));

describe("RootLayout", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should export correct metadata", () => {
    expect(metadata).toEqual({
      title: "P2P Exchange",
      description: "Trade USDT securely with other users",
    });
  });

  it("should use default locale when x-locale header is not present", async () => {
    // Mock the headers implementation
    const mockHeadersList = {
      get: jest.fn().mockImplementation((key) => {
        return key === "x-locale" ? null : null;
      }),
    };
    (headers as jest.Mock).mockResolvedValue(mockHeadersList);

    // Generate the component
    const result = await RootLayout({
      children: <div data-testid="test-children">Test Content</div>,
    });

    // Extract props from the JSX elements
    const htmlProps = result.props;
    const bodyProps = result.props.children.props;
    const childrenContent = result.props.children.props.children;

    // Assertions
    expect(headers).toHaveBeenCalled();
    expect(mockHeadersList.get).toHaveBeenCalledWith("x-locale");
    expect(htmlProps.lang).toBe("en");
    expect(bodyProps.className).toBe("className-mock");
    expect(bodyProps.suppressHydrationWarning).toBe(true);
    expect(childrenContent).toEqual(
      <div data-testid="test-children">Test Content</div>,
    );
  });

  it("should use specific locale when x-locale header is present", async () => {
    // Mock the headers implementation
    const mockHeadersList = {
      get: jest.fn().mockImplementation((key) => {
        return key === "x-locale" ? "fr" : null;
      }),
    };
    (headers as jest.Mock).mockResolvedValue(mockHeadersList);

    // Generate the component
    const result = await RootLayout({
      children: <div data-testid="test-children">Test Content</div>,
    });

    // Extract props from the JSX elements
    const htmlProps = result.props;
    const bodyProps = result.props.children.props;
    const childrenContent = result.props.children.props.children;

    // Assertions
    expect(headers).toHaveBeenCalled();
    expect(mockHeadersList.get).toHaveBeenCalledWith("x-locale");
    expect(htmlProps.lang).toBe("fr");
    expect(bodyProps.className).toBe("className-mock");
    expect(bodyProps.suppressHydrationWarning).toBe(true);
    expect(childrenContent).toEqual(
      <div data-testid="test-children">Test Content</div>,
    );
  });
});
