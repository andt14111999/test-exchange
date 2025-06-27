"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface TokenSelectProps {
  value: string;
  onValueChange: (value: string) => void;
  tokens: string[];
  placeholder?: string;
  disabled?: boolean;
}

export function TokenSelect({
  value,
  onValueChange,
  tokens,
  placeholder = "Select token",
  disabled = false,
}: TokenSelectProps) {
  return (
    <Select value={value} onValueChange={onValueChange} disabled={disabled}>
      <SelectTrigger className="w-full">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {tokens.map((token) => (
          <SelectItem key={token} value={token}>
            {token}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
