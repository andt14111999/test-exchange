"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";

interface SlippageOption {
  value: number; // Basis points (0 = auto, 50 = 0.5%, 100 = 1%, 200 = 2%, 500 = 5%)
  display: string;
}

const SLIPPAGE_OPTIONS: SlippageOption[] = [
  { value: 100, display: "Auto" },
  { value: 0.5, display: "0.5%" },
  { value: 1, display: "1%" },
  { value: 2, display: "2%" },
  { value: 5, display: "5%" },
];

interface SlippageSelectorProps {
  initialValue: number; // Basis points
  onChange: (value: number) => void;
}

export function SlippageSelector({
  initialValue = 100, // Default to 1%
  onChange,
}: SlippageSelectorProps) {
  const t = useTranslations();
  const [selected, setSelected] = useState<number>(initialValue);

  // Đảm bảo rằng initialValue được cập nhật khi prop thay đổi
  useEffect(() => {
    setSelected(initialValue);
  }, [initialValue]);

  // Xử lý khi người dùng chọn một tùy chọn
  const handleSelect = (basisPoints: number) => {
    setSelected(basisPoints);
    onChange(basisPoints);
  };

  return (
    <div className="space-y-2">
      <h3 className="text-lg font-medium">
        {t("liquidity.slippageTolerance")}
      </h3>
      <div className="flex space-x-2">
        {SLIPPAGE_OPTIONS.map((option) => (
          <button
            key={option.value}
            className={`px-4 py-2 rounded-md text-sm font-medium ${
              selected === option.value
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground"
            }`}
            onClick={() => handleSelect(option.value)}
          >
            {option.display}
          </button>
        ))}
      </div>
    </div>
  );
}
