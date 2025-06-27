import { render, screen, fireEvent } from "@testing-library/react";
import { Input } from "@/components/ui/input";
import "@testing-library/jest-dom";

describe("Input Component", () => {
  it("renders correctly with default props", () => {
    render(<Input />);
    const input = screen.getByRole("textbox");
    expect(input).toBeInTheDocument();
    expect(input).toHaveClass(
      "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
    );
  });

  it("applies custom className correctly", () => {
    const customClass = "custom-class";
    render(<Input className={customClass} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveClass(customClass);
  });

  it("forwards ref correctly", () => {
    const ref = jest.fn();
    render(<Input ref={ref} />);
    expect(ref).toHaveBeenCalled();
  });

  it("handles value changes correctly", () => {
    const handleChange = jest.fn();
    render(<Input onChange={handleChange} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "test value" } });
    expect(handleChange).toHaveBeenCalled();
  });

  it("handles disabled state correctly", () => {
    render(<Input disabled />);
    const input = screen.getByRole("textbox");
    expect(input).toBeDisabled();
  });

  it("handles placeholder correctly", () => {
    const placeholder = "Enter text here";
    render(<Input placeholder={placeholder} />);
    const input = screen.getByPlaceholderText(placeholder);
    expect(input).toBeInTheDocument();
  });

  it("handles different input types correctly", () => {
    render(<Input type="password" />);
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("type", "password");
  });

  it("handles NaN value correctly", () => {
    render(<Input value={NaN} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("");
  });

  it("handles empty string value correctly", () => {
    render(<Input value="" />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("");
  });

  it("handles undefined value correctly", () => {
    render(<Input value={undefined} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("");
  });

  it("handles valid number value correctly", () => {
    render(<Input value={42} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("42");
  });

  it("handles focus and blur events correctly", () => {
    const handleFocus = jest.fn();
    const handleBlur = jest.fn();
    render(<Input onFocus={handleFocus} onBlur={handleBlur} />);
    const input = screen.getByRole("textbox");

    fireEvent.focus(input);
    expect(handleFocus).toHaveBeenCalled();

    fireEvent.blur(input);
    expect(handleBlur).toHaveBeenCalled();
  });

  it("handles file input type correctly", () => {
    render(<Input type="file" />);
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("type", "file");
    expect(input).toHaveClass("file:border-0", "file:bg-transparent");
  });
});
