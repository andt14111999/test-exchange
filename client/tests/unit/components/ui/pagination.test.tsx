import { render, screen, fireEvent } from "@testing-library/react";
import { Pagination } from "@/components/ui/pagination";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(
    () => (key: string, values?: { current: number; total: number }) => {
      if (key === "page") {
        return `Page ${values?.current} of ${values?.total}`;
      }
      return key;
    },
  ),
}));

describe("Pagination", () => {
  const defaultProps = {
    currentPage: 2,
    totalPages: 5,
    onPageChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly with default props", () => {
    const { container } = render(<Pagination {...defaultProps} />);

    // Check if navigation buttons are rendered
    expect(
      screen.getByRole("button", { name: "Previous page" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Next page" }),
    ).toBeInTheDocument();

    // Check container layout classes
    const mainContainer = container.firstChild as HTMLElement;
    expect(mainContainer).toHaveClass("flex", "items-center", "gap-1");

    // Check page numbers are rendered
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();

    // Check current page has default variant
    const currentPageButton = screen.getByText("2");
    expect(currentPageButton).toHaveClass(
      "bg-primary",
      "text-primary-foreground",
    );
  });

  it("handles previous page navigation", () => {
    render(<Pagination {...defaultProps} />);

    const previousButton = screen.getByRole("button", {
      name: "Previous page",
    });
    fireEvent.click(previousButton);

    expect(defaultProps.onPageChange).toHaveBeenCalledWith(1);
    expect(defaultProps.onPageChange).toHaveBeenCalledTimes(1);
  });

  it("handles next page navigation", () => {
    render(<Pagination {...defaultProps} />);

    const nextButton = screen.getByRole("button", { name: "Next page" });
    fireEvent.click(nextButton);

    expect(defaultProps.onPageChange).toHaveBeenCalledWith(3);
    expect(defaultProps.onPageChange).toHaveBeenCalledTimes(1);
  });

  it("disables previous button on first page", () => {
    render(<Pagination {...defaultProps} currentPage={1} />);

    const previousButton = screen.getByRole("button", {
      name: "Previous page",
    });
    expect(previousButton).toBeDisabled();

    // Verify click on disabled button doesn't trigger onPageChange
    fireEvent.click(previousButton);
    expect(defaultProps.onPageChange).not.toHaveBeenCalled();
  });

  it("disables next button on last page", () => {
    render(<Pagination {...defaultProps} currentPage={5} />);

    const nextButton = screen.getByRole("button", { name: "Next page" });
    expect(nextButton).toBeDisabled();

    // Verify click on disabled button doesn't trigger onPageChange
    fireEvent.click(nextButton);
    expect(defaultProps.onPageChange).not.toHaveBeenCalled();
  });

  it("renders with single page", () => {
    render(<Pagination {...defaultProps} currentPage={1} totalPages={1} />);

    expect(screen.getByText("1")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Previous page" }),
    ).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next page" })).toBeDisabled();

    // Verify clicks on disabled buttons don't trigger onPageChange
    fireEvent.click(screen.getByRole("button", { name: "Previous page" }));
    fireEvent.click(screen.getByRole("button", { name: "Next page" }));
    expect(defaultProps.onPageChange).not.toHaveBeenCalled();
  });

  it("handles edge case with zero total pages", () => {
    render(<Pagination {...defaultProps} currentPage={1} totalPages={0} />);

    const previousButton = screen.getByRole("button", {
      name: "Previous page",
    });
    const nextButton = screen.getByRole("button", { name: "Next page" });

    expect(previousButton).toBeDisabled();
    expect(nextButton).toBeDisabled();

    // Verify clicks on disabled buttons don't trigger onPageChange
    fireEvent.click(previousButton);
    fireEvent.click(nextButton);
    expect(defaultProps.onPageChange).not.toHaveBeenCalled();
  });

  it("shows ellipsis and last page when total pages > 5", () => {
    render(<Pagination {...defaultProps} currentPage={2} totalPages={10} />);

    // Check first 5 pages are shown
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();

    // Check ellipsis and last page are shown
    expect(screen.getByText("...")).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
  });

  it("handles page number click", () => {
    render(<Pagination {...defaultProps} />);

    const pageThreeButton = screen.getByText("3");
    fireEvent.click(pageThreeButton);

    expect(defaultProps.onPageChange).toHaveBeenCalledWith(3);
    expect(defaultProps.onPageChange).toHaveBeenCalledTimes(1);
  });

  it("handles last page click when total pages > 5", () => {
    render(<Pagination {...defaultProps} currentPage={2} totalPages={10} />);

    const lastPageButton = screen.getByText("10");
    fireEvent.click(lastPageButton);

    expect(defaultProps.onPageChange).toHaveBeenCalledWith(10);
    expect(defaultProps.onPageChange).toHaveBeenCalledTimes(1);
  });
});
