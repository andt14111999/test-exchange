import React from "react";
import { render } from "@testing-library/react";
import HomePage from "@/app/[locale]/page";

// Mock HomeContent
jest.mock("@/components/home-content", () => ({
  HomeContent: () => <div data-testid="home-content" />,
}));

describe("HomePage", () => {
  it("should render HomeContent", () => {
    const { getByTestId, container } = render(<HomePage />);
    expect(getByTestId("home-content")).toBeInTheDocument();
    expect(container).toMatchSnapshot();
  });
});
