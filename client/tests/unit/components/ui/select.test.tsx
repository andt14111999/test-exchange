import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  Select,
  SelectGroup,
  SelectValue,
  SelectTrigger,
  SelectContent,
  SelectLabel,
  SelectItem,
  SelectSeparator,
} from "@/components/ui/select";

describe("Select components", () => {
  describe("SelectTrigger", () => {
    it("should render correctly", () => {
      render(
        <Select>
          <SelectTrigger>
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
        </Select>,
      );
      const trigger = screen.getByRole("combobox");
      expect(trigger).toBeInTheDocument();
      expect(trigger).toHaveClass(
        "flex h-10 w-full items-center justify-between",
      );
    });

    it("should apply custom className", () => {
      render(
        <Select>
          <SelectTrigger className="custom-class">
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
        </Select>,
      );
      const trigger = screen.getByRole("combobox");
      expect(trigger).toHaveClass("custom-class");
    });

    it("should handle disabled state", () => {
      render(
        <Select>
          <SelectTrigger disabled>
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
        </Select>,
      );
      const trigger = screen.getByRole("combobox");
      expect(trigger).toBeDisabled();
      expect(trigger).toHaveClass("disabled:cursor-not-allowed");
      expect(trigger).toHaveClass("disabled:opacity-50");
    });
  });

  describe("SelectContent", () => {
    it("should render with default position", () => {
      render(
        <Select open>
          <SelectTrigger>
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="test">Test</SelectItem>
          </SelectContent>
        </Select>,
      );
      const content = screen.getByRole("listbox");
      expect(content).toBeInTheDocument();
      expect(content).toHaveClass("relative z-50");
    });

    it("should apply custom className", () => {
      render(
        <Select open>
          <SelectTrigger>
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
          <SelectContent className="custom-class">
            <SelectItem value="test">Test</SelectItem>
          </SelectContent>
        </Select>,
      );
      const content = screen.getByRole("listbox");
      expect(content).toHaveClass("custom-class");
    });
  });

  describe("SelectItem", () => {
    it("should render correctly", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectItem value="test">Test Item</SelectItem>
          </SelectContent>
        </Select>,
      );
      const item = screen.getByRole("option", { name: "Test Item" });
      expect(item).toBeInTheDocument();
      expect(item).toHaveClass(
        "relative flex w-full cursor-default select-none",
      );
    });

    it("should handle disabled state", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectItem value="test" disabled>
              Test Item
            </SelectItem>
          </SelectContent>
        </Select>,
      );
      const item = screen.getByRole("option", { name: "Test Item (Đã tắt)" });
      expect(item).toHaveAttribute("data-disabled");
      expect(item).toHaveClass("data-[disabled]:pointer-events-none");
      expect(item).toHaveClass("data-[disabled]:opacity-70");
    });

    it("should apply custom className", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectItem value="test" className="custom-class">
              Test Item
            </SelectItem>
          </SelectContent>
        </Select>,
      );
      const item = screen.getByRole("option", { name: "Test Item" });
      expect(item).toHaveClass("custom-class");
    });
  });

  describe("SelectLabel", () => {
    it("should render correctly", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectGroup>
              <SelectLabel>Test Label</SelectLabel>
            </SelectGroup>
          </SelectContent>
        </Select>,
      );
      const label = screen.getByText("Test Label");
      expect(label).toBeInTheDocument();
      expect(label).toHaveClass("py-1.5 pl-8 pr-2 text-sm font-semibold");
    });

    it("should apply custom className", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectGroup>
              <SelectLabel className="custom-class">Test Label</SelectLabel>
            </SelectGroup>
          </SelectContent>
        </Select>,
      );
      const label = screen.getByText("Test Label");
      expect(label).toHaveClass("custom-class");
    });
  });

  describe("SelectGroup", () => {
    it("should render correctly", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectGroup>
              <SelectLabel>Group Label</SelectLabel>
              <SelectItem value="test">Test Item</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>,
      );
      const group = screen.getByRole("group");
      expect(group).toBeInTheDocument();
      expect(screen.getByText("Group Label")).toBeInTheDocument();
      expect(
        screen.getByRole("option", { name: "Test Item" }),
      ).toBeInTheDocument();
    });
  });

  describe("SelectSeparator", () => {
    it("should render correctly", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectGroup>
              <SelectItem value="test1">Test Item 1</SelectItem>
              <SelectSeparator />
              <SelectItem value="test2">Test Item 2</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>,
      );
      const separator = screen.getByTestId("select-separator");
      expect(separator).toBeInTheDocument();
      expect(separator).toHaveClass("-mx-1 my-1 h-px bg-muted");
    });

    it("should apply custom className", () => {
      render(
        <Select open>
          <SelectContent>
            <SelectGroup>
              <SelectItem value="test1">Test Item 1</SelectItem>
              <SelectSeparator className="custom-class" />
              <SelectItem value="test2">Test Item 2</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>,
      );
      const separator = screen.getByTestId("select-separator");
      expect(separator).toHaveClass("custom-class");
    });
  });

  describe("Integration tests", () => {
    it("should handle selection", () => {
      const onValueChange = jest.fn();
      render(
        <Select onValueChange={onValueChange}>
          <SelectTrigger>
            <SelectValue placeholder="Select option" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="test">Test Item</SelectItem>
          </SelectContent>
        </Select>,
      );

      const trigger = screen.getByRole("combobox");
      fireEvent.click(trigger);

      const option = screen.getByRole("option", { name: "Test Item" });
      fireEvent.click(option);

      expect(onValueChange).toHaveBeenCalledWith("test");
    });

    it("should update trigger value after selection", () => {
      render(
        <Select defaultValue="test">
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="test">Test Item</SelectItem>
          </SelectContent>
        </Select>,
      );

      const trigger = screen.getByRole("combobox");
      expect(trigger).toHaveTextContent("Test Item");
    });
  });
});
