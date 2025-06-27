import * as React from "react";
import { cn } from "@/lib/utils";

interface NumberInputWithCommasProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  step?: string | number;
  min?: string | number;
  max?: string | number;
  inputMode?: "decimal" | "numeric" | "text";
  "data-testid"?: string;
}

export const NumberInputWithCommas = React.forwardRef<
  HTMLInputElement,
  NumberInputWithCommasProps
>(
  (
    {
      value,
      onChange,
      placeholder,
      disabled,
      className,
      step,
      min,
      max,
      "data-testid": dataTestId,
    },
    ref,
  ) => {
    // Format number with commas for display, preserving decimals and leading dot
    const formatNumberWithCommas = (value: string) => {
      if (!value) return "";
      if (value.startsWith(".")) {
        const decimals = value.slice(1);
        return "." + decimals.replace(/(\d{3})(?=\d)/g, "$1");
      }
      const [intPart, decPart] = value.split(".");
      const intWithCommas = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
      return decPart !== undefined
        ? `${intWithCommas}.${decPart}`
        : intWithCommas;
    };

    const removeCommas = (value: string) => value.replace(/,/g, "");

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const inputValue = e.target.value;
      const rawValue = removeCommas(inputValue);
      if (
        rawValue === "" ||
        /^\d*\.?\d*$/.test(rawValue) ||
        /^\.\d*$/.test(rawValue)
      ) {
        onChange(rawValue);
      }
    };

    return (
      <input
        ref={ref}
        type="text"
        value={formatNumberWithCommas(value)}
        onChange={handleChange}
        placeholder={placeholder}
        disabled={disabled}
        className={cn(
          "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
          className,
        )}
        step={step}
        min={min}
        max={max}
        inputMode="decimal"
        autoComplete="off"
        data-testid={dataTestId}
      />
    );
  },
);
NumberInputWithCommas.displayName = "NumberInputWithCommas";
