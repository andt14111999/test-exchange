import * as React from "react";
import { render, screen } from "@testing-library/react";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";

describe("Table Components", () => {
  describe("Table", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <tbody>
            <tr>
              <td>Content</td>
            </tr>
          </tbody>
        </Table>,
      );
      const wrapper = screen.getByRole("table").parentElement;
      expect(wrapper).toHaveClass("relative", "w-full", "overflow-auto");
      const table = screen.getByRole("table");
      expect(table).toHaveClass("w-full", "caption-bottom", "text-sm");
    });

    it("applies custom className", () => {
      render(
        <Table className="custom-class">
          <tbody>
            <tr>
              <td>Content</td>
            </tr>
          </tbody>
        </Table>,
      );
      const table = screen.getByRole("table");
      expect(table).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableElement>();
      render(
        <Table ref={ref}>
          <tbody>
            <tr>
              <td>Content</td>
            </tr>
          </tbody>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableElement);
    });
  });

  describe("TableHeader", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <TableHeader data-testid="header">
            <tr>
              <th>Header</th>
            </tr>
          </TableHeader>
        </Table>,
      );
      const header = screen.getByTestId("header");
      expect(header).toHaveClass("[&_tr]:border-b");
    });

    it("applies custom className", () => {
      render(
        <Table>
          <TableHeader className="custom-class" data-testid="header">
            <tr>
              <th>Header</th>
            </tr>
          </TableHeader>
        </Table>,
      );
      const header = screen.getByTestId("header");
      expect(header).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableSectionElement>();
      render(
        <Table>
          <TableHeader ref={ref}>
            <tr>
              <th>Header</th>
            </tr>
          </TableHeader>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableSectionElement);
    });
  });

  describe("TableBody", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <TableBody data-testid="body">
            <tr>
              <td>Content</td>
            </tr>
          </TableBody>
        </Table>,
      );
      const body = screen.getByTestId("body");
      expect(body).toHaveClass("[&_tr:last-child]:border-0");
    });

    it("applies custom className", () => {
      render(
        <Table>
          <TableBody className="custom-class" data-testid="body">
            <tr>
              <td>Content</td>
            </tr>
          </TableBody>
        </Table>,
      );
      const body = screen.getByTestId("body");
      expect(body).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableSectionElement>();
      render(
        <Table>
          <TableBody ref={ref}>
            <tr>
              <td>Content</td>
            </tr>
          </TableBody>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableSectionElement);
    });
  });

  describe("TableRow", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <TableBody>
            <TableRow data-testid="row">
              <td>Content</td>
            </TableRow>
          </TableBody>
        </Table>,
      );
      const row = screen.getByTestId("row");
      expect(row).toHaveClass(
        "border-b",
        "transition-colors",
        "hover:bg-muted/50",
        "data-[state=selected]:bg-muted",
      );
    });

    it("applies custom className", () => {
      render(
        <Table>
          <TableBody>
            <TableRow className="custom-class" data-testid="row">
              <td>Content</td>
            </TableRow>
          </TableBody>
        </Table>,
      );
      const row = screen.getByTestId("row");
      expect(row).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableRowElement>();
      render(
        <Table>
          <TableBody>
            <TableRow ref={ref}>
              <td>Content</td>
            </TableRow>
          </TableBody>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableRowElement);
    });
  });

  describe("TableHead", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <TableHeader>
            <tr>
              <TableHead data-testid="head">Header</TableHead>
            </tr>
          </TableHeader>
        </Table>,
      );
      const head = screen.getByTestId("head");
      expect(head).toHaveClass(
        "h-12",
        "px-4",
        "text-left",
        "align-middle",
        "font-medium",
        "text-muted-foreground",
        "[&:has([role=checkbox])]:pr-0",
      );
    });

    it("applies custom className", () => {
      render(
        <Table>
          <TableHeader>
            <tr>
              <TableHead className="custom-class" data-testid="head">
                Header
              </TableHead>
            </tr>
          </TableHeader>
        </Table>,
      );
      const head = screen.getByTestId("head");
      expect(head).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableCellElement>();
      render(
        <Table>
          <TableHeader>
            <tr>
              <TableHead ref={ref}>Header</TableHead>
            </tr>
          </TableHeader>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableCellElement);
    });
  });

  describe("TableCell", () => {
    it("renders with default styles", () => {
      render(
        <Table>
          <TableBody>
            <TableRow>
              <TableCell data-testid="cell">Content</TableCell>
            </TableRow>
          </TableBody>
        </Table>,
      );
      const cell = screen.getByTestId("cell");
      expect(cell).toHaveClass(
        "p-4",
        "align-middle",
        "[&:has([role=checkbox])]:pr-0",
      );
    });

    it("applies custom className", () => {
      render(
        <Table>
          <TableBody>
            <TableRow>
              <TableCell className="custom-class" data-testid="cell">
                Content
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>,
      );
      const cell = screen.getByTestId("cell");
      expect(cell).toHaveClass("custom-class");
    });

    it("forwards ref correctly", () => {
      const ref = React.createRef<HTMLTableCellElement>();
      render(
        <Table>
          <TableBody>
            <TableRow>
              <TableCell ref={ref}>Content</TableCell>
            </TableRow>
          </TableBody>
        </Table>,
      );
      expect(ref.current).toBeInstanceOf(HTMLTableCellElement);
    });
  });

  describe("Integration", () => {
    it("renders a complete table structure", () => {
      render(
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Age</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>John</TableCell>
              <TableCell>25</TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Jane</TableCell>
              <TableCell>30</TableCell>
            </TableRow>
          </TableBody>
        </Table>,
      );

      expect(screen.getByText("Name")).toBeInTheDocument();
      expect(screen.getByText("Age")).toBeInTheDocument();
      expect(screen.getByText("John")).toBeInTheDocument();
      expect(screen.getByText("25")).toBeInTheDocument();
      expect(screen.getByText("Jane")).toBeInTheDocument();
      expect(screen.getByText("30")).toBeInTheDocument();
    });
  });
});
