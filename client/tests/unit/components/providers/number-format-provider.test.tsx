import { render, act, RenderResult } from "@testing-library/react";
import {
  NumberFormatProvider,
  useNumberFormat,
} from "@/components/providers/number-format-provider";
import BigNumber from "bignumber.js";
import * as React from "react";

// Mock React
jest.mock("react", () => ({
  ...jest.requireActual("react"),
  useState: jest.fn(),
}));

// Mock Intl.NumberFormat
const mockFormat = jest.fn();
const mockNumberFormat = jest.fn(() => ({
  format: mockFormat,
})) as jest.Mock & { supportedLocalesOf: jest.Mock };
mockNumberFormat.supportedLocalesOf = jest.fn();

// Store original Intl.NumberFormat
const OriginalIntl = global.Intl;

describe("NumberFormatProvider", () => {
  // Mock implementations
  const originalNavigator = global.navigator;
  let setIsHydrated: jest.Mock;
  let setLocale: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    // Mock useState
    setIsHydrated = jest.fn();
    setLocale = jest.fn();
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [false, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    // Mock Intl.NumberFormat
    global.Intl = {
      ...OriginalIntl,
      NumberFormat: mockNumberFormat as unknown as typeof Intl.NumberFormat,
    };

    // Mock navigator
    Object.defineProperty(global, "navigator", {
      value: {
        language: "en-US",
      },
      writable: true,
    });

    // Reset mock implementations
    mockFormat.mockImplementation((value) => value.toString());
  });

  afterEach(() => {
    // Restore original implementations
    global.Intl = OriginalIntl;
    Object.defineProperty(global, "navigator", {
      value: originalNavigator,
      writable: true,
    });
    jest.useRealTimers();
  });

  const TestComponent = () => {
    const { isHydrated, locale, formatNumber } = useNumberFormat();
    return (
      <div>
        <div data-testid="hydration-status">{isHydrated.toString()}</div>
        <div data-testid="locale">{locale}</div>
        <div data-testid="formatted-number">
          {formatNumber(1234.5678, { decimals: 2 })}
        </div>
        <div data-testid="formatted-currency">
          {formatNumber(1234.5678, { currency: "VND", decimals: 2 })}
        </div>
        <div data-testid="formatted-currency-no-symbol">
          {formatNumber(1234.5678, {
            currency: "VND",
            decimals: 2,
            showSymbol: false,
          })}
        </div>
        <div data-testid="formatted-big-number">
          {formatNumber(new BigNumber("1234.5678"), { decimals: 2 })}
        </div>
        <div data-testid="formatted-string-number">
          {formatNumber("1234.5678", { decimals: 2 })}
        </div>
        <div data-testid="invalid-number">
          {formatNumber("invalid", { decimals: 2 })}
        </div>
      </div>
    );
  };

  const TestCurrencyComponent = ({ currency }: { currency: string }) => {
    const { formatNumber } = useNumberFormat();
    return (
      <div data-testid={`currency-${currency}`}>
        {formatNumber(1234.5678, { currency, decimals: 2 })}
      </div>
    );
  };

  it("should initialize with default values", async () => {
    let container: RenderResult;

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    // Verify initial state before any effects run
    expect(container!.getByTestId("hydration-status")).toHaveTextContent(
      "false",
    );
    expect(container!.getByTestId("locale")).toHaveTextContent("en-US");
  });

  it("should update hydration status and locale after mount", async () => {
    let container: RenderResult;

    // Mock useState to return true for isHydrated after effect
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("hydration-status")).toHaveTextContent(
      "true",
    );
    expect(container!.getByTestId("locale")).toHaveTextContent("en-US");
  });

  it("should format numbers correctly before hydration", async () => {
    let container: RenderResult;

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("formatted-number")).toHaveTextContent(
      "1234.5678",
    );
    expect(container!.getByTestId("formatted-currency")).toHaveTextContent(
      "₫1234.5678",
    );
  });

  it("should format numbers correctly after hydration", async () => {
    let container: RenderResult;

    mockFormat.mockReturnValue("1,234.57");

    // Mock useState to return true for isHydrated
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("formatted-number")).toHaveTextContent(
      "1,234.57",
    );
    expect(container!.getByTestId("formatted-currency")).toHaveTextContent(
      "₫1,234.57",
    );
    expect(
      container!.getByTestId("formatted-currency-no-symbol"),
    ).toHaveTextContent("1,234.57");
  });

  it("should handle different number types correctly", async () => {
    let container: RenderResult;

    mockFormat.mockReturnValue("1,234.57");

    // Mock useState to return true for isHydrated
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("formatted-big-number")).toHaveTextContent(
      "1,234.57",
    );
    expect(container!.getByTestId("formatted-string-number")).toHaveTextContent(
      "1,234.57",
    );
  });

  it("should handle invalid numbers", async () => {
    let container: RenderResult;

    // Mock format to throw an error for invalid numbers
    mockFormat.mockImplementation(() => {
      throw new Error("Invalid number");
    });

    // Mock useState to return true for isHydrated
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("invalid-number")).toHaveTextContent("0");
  });

  it("should handle different locales", async () => {
    let container: RenderResult;

    // Mock navigator with different locale
    Object.defineProperty(global, "navigator", {
      value: {
        language: "fr-FR",
      },
      writable: true,
    });

    mockFormat.mockReturnValue("1 234,57");

    // Mock useState to return true for isHydrated and fr-FR for locale
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["fr-FR", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestComponent />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("locale")).toHaveTextContent("fr-FR");
    expect(container!.getByTestId("formatted-number")).toHaveTextContent(
      "1 234,57",
    );
  });

  it("should handle all currency symbols", async () => {
    let container: RenderResult;

    mockFormat.mockReturnValue("1,234.57");

    // Mock useState to return true for isHydrated
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    const currencies = ["VND", "PHP", "NGN", "USDT"];
    const symbols = ["₫", "₱", "₦", "$"];

    for (let i = 0; i < currencies.length; i++) {
      const currency = currencies[i];
      const symbol = symbols[i];

      await act(async () => {
        container = render(
          <NumberFormatProvider>
            <TestCurrencyComponent currency={currency} />
          </NumberFormatProvider>,
        );
      });

      expect(container!.getByTestId(`currency-${currency}`)).toHaveTextContent(
        `${symbol}1,234.57`,
      );
    }
  });

  it("should handle unknown currency symbols", async () => {
    let container: RenderResult;

    mockFormat.mockReturnValue("1,234.57");

    // Mock useState to return true for isHydrated
    (jest.spyOn(React, "useState") as jest.SpyInstance).mockImplementation(((
      initialValue: unknown,
    ) => {
      if (typeof initialValue === "boolean") {
        return [true, setIsHydrated];
      }
      return ["en-US", setLocale];
    }) as () => [unknown, typeof setIsHydrated | typeof setLocale]);

    await act(async () => {
      container = render(
        <NumberFormatProvider>
          <TestCurrencyComponent currency="XXX" />
        </NumberFormatProvider>,
      );
    });

    expect(container!.getByTestId("currency-XXX")).toHaveTextContent(
      "1,234.57",
    );
  });
});
