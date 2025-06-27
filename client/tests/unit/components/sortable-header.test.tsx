import { render, screen, fireEvent } from "@testing-library/react";
import { SortableHeader } from "@/components/sortable-header";

describe("SortableHeader", () => {
  const defaultProps = {
    label: "Amount",
    sortKey: "amount" as const,
    currentSort: {
      key: "amount" as const,
      direction: "asc" as const,
    },
    onSort: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with label", () => {
    render(<SortableHeader {...defaultProps} />);
    expect(screen.getByText("Amount")).toBeInTheDocument();
  });

  it("shows up arrow when sort is ascending and active", () => {
    render(<SortableHeader {...defaultProps} />);
    expect(screen.getByTestId("arrow-up-icon")).toBeInTheDocument();
  });

  it("shows down arrow when sort is descending and active", () => {
    render(
      <SortableHeader
        {...defaultProps}
        currentSort={{ key: "amount", direction: "desc" }}
      />,
    );
    expect(screen.getByTestId("arrow-down-icon")).toBeInTheDocument();
  });

  it("does not show any arrow when column is not being sorted", () => {
    render(
      <SortableHeader
        {...defaultProps}
        currentSort={{ key: "status", direction: "asc" }}
      />,
    );
    expect(screen.queryByTestId("arrow-up-icon")).not.toBeInTheDocument();
    expect(screen.queryByTestId("arrow-down-icon")).not.toBeInTheDocument();
  });

  it("calls onSort with correct key when clicked", () => {
    render(<SortableHeader {...defaultProps} />);
    fireEvent.click(screen.getByText("Amount"));
    expect(defaultProps.onSort).toHaveBeenCalledWith("amount");
  });

  it("applies correct styling classes", () => {
    render(<SortableHeader {...defaultProps} />);
    const header = screen.getByRole("columnheader");
    expect(header).toHaveClass("cursor-pointer");
    expect(header).toHaveClass("select-none");
  });

  it("renders correctly for different sort keys", () => {
    const { rerender } = render(
      <SortableHeader
        {...defaultProps}
        label="Status"
        sortKey="status"
        currentSort={{ key: "status", direction: "asc" }}
      />,
    );
    expect(screen.getByText("Status")).toBeInTheDocument();

    rerender(
      <SortableHeader
        {...defaultProps}
        label="Date"
        sortKey="date"
        currentSort={{ key: "date", direction: "desc" }}
      />,
    );
    expect(screen.getByText("Date")).toBeInTheDocument();
  });
});
