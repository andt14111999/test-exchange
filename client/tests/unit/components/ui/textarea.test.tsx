import * as React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Textarea } from "@/components/ui/textarea";

describe("Textarea", () => {
  it("renders textarea with default props", () => {
    render(<Textarea />);
    const textarea = screen.getByRole("textbox");
    expect(textarea).toBeInTheDocument();
    expect(textarea).toHaveClass(
      "flex",
      "min-h-[80px]",
      "w-full",
      "rounded-md",
      "border",
      "border-input",
      "bg-background",
      "px-3",
      "py-2",
      "text-sm",
      "ring-offset-background",
    );
  });

  it("renders textarea with custom className", () => {
    const customClass = "custom-class";
    render(<Textarea className={customClass} />);
    const textarea = screen.getByRole("textbox");
    expect(textarea).toHaveClass(customClass);
  });

  it("forwards ref to textarea element", () => {
    const ref = React.createRef<HTMLTextAreaElement>();
    render(<Textarea ref={ref} />);
    expect(ref.current).toBeInstanceOf(HTMLTextAreaElement);
  });

  it("handles user input", () => {
    const handleChange = jest.fn();
    render(<Textarea onChange={handleChange} />);
    const textarea = screen.getByRole("textbox");

    fireEvent.change(textarea, { target: { value: "test input" } });
    expect(handleChange).toHaveBeenCalled();
  });

  it("applies disabled styles when disabled", () => {
    render(<Textarea disabled />);
    const textarea = screen.getByRole("textbox");
    expect(textarea).toBeDisabled();
    expect(textarea).toHaveClass(
      "disabled:cursor-not-allowed",
      "disabled:opacity-50",
    );
  });

  it("handles placeholder text", () => {
    const placeholder = "Enter text here";
    render(<Textarea placeholder={placeholder} />);
    const textarea = screen.getByPlaceholderText(placeholder);
    expect(textarea).toBeInTheDocument();
  });

  it("handles focus events", () => {
    const handleFocus = jest.fn();
    const handleBlur = jest.fn();
    render(<Textarea onFocus={handleFocus} onBlur={handleBlur} />);
    const textarea = screen.getByRole("textbox");

    fireEvent.focus(textarea);
    expect(handleFocus).toHaveBeenCalled();

    fireEvent.blur(textarea);
    expect(handleBlur).toHaveBeenCalled();
  });

  it("applies focus-visible styles on focus", () => {
    render(<Textarea />);
    const textarea = screen.getByRole("textbox");
    expect(textarea).toHaveClass(
      "focus-visible:outline-none",
      "focus-visible:ring-2",
      "focus-visible:ring-ring",
      "focus-visible:ring-offset-2",
    );
  });
});
