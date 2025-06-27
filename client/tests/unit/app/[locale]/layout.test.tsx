import React from "react";
import { render } from "@testing-library/react";
import LocaleLayout from "@/app/[locale]/layout";
import { getMessages } from "next-intl/server";
import { defaultLocale } from "@/config/i18n";

// Mock next-intl
jest.mock("next-intl/server", () => ({
  getMessages: jest.fn(),
  getRequestConfig: jest.fn(),
}));

// Mock next-intl client provider
jest.mock("next-intl", () => ({
  NextIntlClientProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="next-intl-provider">{children}</div>
  ),
}));

// Mock all providers
jest.mock("@/components/providers/number-format-provider", () => ({
  NumberFormatProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="number-format-provider">{children}</div>
  ),
}));

jest.mock("@/components/providers/query-provider", () => ({
  QueryProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="query-provider">{children}</div>
  ),
}));

jest.mock("@/components/providers/client-pool-initializer", () => ({
  ClientPoolInitializer: () => <div data-testid="client-pool-initializer" />,
}));

jest.mock("@/components/providers/notification-provider", () => ({
  NotificationProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="notification-provider">{children}</div>
  ),
}));

jest.mock("@/components/providers/balance-provider", () => ({
  BalanceProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="balance-provider">{children}</div>
  ),
}));

jest.mock("@/components/layout/header", () => ({
  Header: () => <div data-testid="header" />,
}));

jest.mock("@/components/ui/toast", () => ({
  Toast: () => <div data-testid="toast" />,
}));

describe("LocaleLayout", () => {
  const mockMessages = { test: "test" };
  const mockChildren = <div data-testid="test-children">Test Content</div>;

  beforeEach(() => {
    jest.clearAllMocks();
    (getMessages as jest.Mock).mockResolvedValue(mockMessages);
  });

  it("should render with valid locale", async () => {
    const params = Promise.resolve({ locale: "en" });
    const { container } = render(
      await LocaleLayout({ children: mockChildren, params }),
    );

    expect(getMessages).toHaveBeenCalledWith({ locale: "en" });
    expect(container).toMatchSnapshot();
  });

  it("should use default locale when locale is invalid", async () => {
    const params = Promise.resolve({ locale: "invalid" });
    const { container } = render(
      await LocaleLayout({ children: mockChildren, params }),
    );

    expect(getMessages).toHaveBeenCalledWith({ locale: defaultLocale });
    expect(container).toMatchSnapshot();
  });

  it("should use default locale when locale is empty", async () => {
    const params = Promise.resolve({ locale: "" });
    const { container } = render(
      await LocaleLayout({ children: mockChildren, params }),
    );

    expect(getMessages).toHaveBeenCalledWith({ locale: defaultLocale });
    expect(container).toMatchSnapshot();
  });

  it("should render all required components in correct order", async () => {
    const params = Promise.resolve({ locale: "en" });
    const { getByTestId } = render(
      await LocaleLayout({ children: mockChildren, params }),
    );

    expect(getByTestId("next-intl-provider")).toBeInTheDocument();
    expect(getByTestId("number-format-provider")).toBeInTheDocument();
    expect(getByTestId("query-provider")).toBeInTheDocument();
    expect(getByTestId("client-pool-initializer")).toBeInTheDocument();
    expect(getByTestId("notification-provider")).toBeInTheDocument();
    expect(getByTestId("balance-provider")).toBeInTheDocument();
    expect(getByTestId("header")).toBeInTheDocument();
    expect(getByTestId("toast")).toBeInTheDocument();
    expect(getByTestId("test-children")).toBeInTheDocument();
  });
});
