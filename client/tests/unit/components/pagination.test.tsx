import { render, screen, fireEvent } from "@testing-library/react";
import { Pagination } from "@/components/pagination";
import { useTranslations } from "next-intl";

// Mock the next-intl hooks
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

describe("Pagination", () => {
  const mockTranslate = jest.fn((key, values) => {
    if (key === "page") {
      return `Page ${values.current} of ${values.total}`;
    }
    return key;
  });

  beforeEach(() => {
    jest.clearAllMocks();
    (useTranslations as jest.Mock).mockReturnValue(mockTranslate);
  });

  it("renders pagination with correct page information", () => {
    render(
      <Pagination currentPage={2} totalPages={5} onPageChange={() => {}} />,
    );

    expect(screen.getByText("Page 2 of 5")).toBeInTheDocument();
    expect(screen.getByText("previous")).toBeInTheDocument();
    expect(screen.getByText("next")).toBeInTheDocument();
  });

  it("disables previous button on first page", () => {
    render(
      <Pagination currentPage={1} totalPages={5} onPageChange={() => {}} />,
    );

    const previousButton = screen.getByText("previous");
    expect(previousButton).toBeDisabled();
    expect(screen.getByText("next")).not.toBeDisabled();
  });

  it("disables next button on last page", () => {
    render(
      <Pagination currentPage={5} totalPages={5} onPageChange={() => {}} />,
    );

    const nextButton = screen.getByText("next");
    expect(nextButton).toBeDisabled();
    expect(screen.getByText("previous")).not.toBeDisabled();
  });

  it("disables both buttons when there are no pages", () => {
    render(
      <Pagination currentPage={1} totalPages={0} onPageChange={() => {}} />,
    );

    expect(screen.getByText("previous")).toBeDisabled();
    expect(screen.getByText("next")).toBeDisabled();
  });

  it("calls onPageChange with correct page number when previous is clicked", () => {
    const handlePageChange = jest.fn();
    render(
      <Pagination
        currentPage={3}
        totalPages={5}
        onPageChange={handlePageChange}
      />,
    );

    fireEvent.click(screen.getByText("previous"));
    expect(handlePageChange).toHaveBeenCalledWith(2);
  });

  it("calls onPageChange with correct page number when next is clicked", () => {
    const handlePageChange = jest.fn();
    render(
      <Pagination
        currentPage={3}
        totalPages={5}
        onPageChange={handlePageChange}
      />,
    );

    fireEvent.click(screen.getByText("next"));
    expect(handlePageChange).toHaveBeenCalledWith(4);
  });

  it("uses translations from transactions namespace", () => {
    render(
      <Pagination currentPage={1} totalPages={5} onPageChange={() => {}} />,
    );

    expect(useTranslations).toHaveBeenCalledWith("transactions");
    expect(mockTranslate).toHaveBeenCalledWith("page", {
      current: 1,
      total: 5,
    });
    expect(mockTranslate).toHaveBeenCalledWith("previous");
    expect(mockTranslate).toHaveBeenCalledWith("next");
  });
});
