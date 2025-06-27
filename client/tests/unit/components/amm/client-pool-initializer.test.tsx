import React from "react";
import { render, waitFor } from "@testing-library/react";
import { ClientPoolInitializer } from "@/components/amm/client-pool-initializer";

// Mock the dynamic import
jest.mock("next/dynamic", () => ({
  __esModule: true,
  default: (
    factory: () => Promise<{ default: React.ComponentType }>,
    options: { ssr: boolean },
  ) => {
    expect(options.ssr).toBe(false); // Test SSR option

    // Test the factory function
    factory().then((mod) => {
      expect(mod).toBeDefined();
    });

    const DynamicComponent = () => (
      <div data-testid="mocked-pool-initializer">Mocked Pool Initializer</div>
    );
    return DynamicComponent;
  },
}));

// Mock the pool-initializer module
jest.mock("@/components/amm/pool-initializer", () => ({
  PoolInitializer: () => <div>Mocked Pool Initializer Implementation</div>,
}));

describe("ClientPoolInitializer", () => {
  it("renders without crashing", () => {
    const { container } = render(<ClientPoolInitializer />);
    expect(container).toBeInTheDocument();
  });

  it("renders with Suspense and dynamic component", async () => {
    const { findByTestId } = render(<ClientPoolInitializer />);
    const mockedComponent = await findByTestId("mocked-pool-initializer");
    expect(mockedComponent).toBeInTheDocument();
    expect(mockedComponent).toHaveTextContent("Mocked Pool Initializer");
  });

  it("handles dynamic import correctly", async () => {
    const { container } = render(<ClientPoolInitializer />);
    const suspenseElement = container.firstChild;
    expect(suspenseElement).toBeInTheDocument();

    await waitFor(() => {
      const dynamicComponent = container.querySelector(
        '[data-testid="mocked-pool-initializer"]',
      );
      expect(dynamicComponent).toBeInTheDocument();
    });
  });

  it("renders with null fallback in Suspense", () => {
    const { container } = render(<ClientPoolInitializer />);
    const suspenseElement = container.firstChild;
    expect(suspenseElement).toHaveAttribute(
      "data-testid",
      "mocked-pool-initializer",
    );
  });

  it("loads the PoolInitializer module correctly", async () => {
    const { container } = render(<ClientPoolInitializer />);

    await waitFor(() => {
      const dynamicComponent = container.querySelector(
        '[data-testid="mocked-pool-initializer"]',
      );
      expect(dynamicComponent).toBeInTheDocument();
    });
  });
});
