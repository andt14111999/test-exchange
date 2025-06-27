"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { FormattedPool } from "@/lib/api/pools";
import { TickMath } from "@/lib/amm/tick-math";
import { useTranslations } from "next-intl";
import { formatDisplayPrice } from "@/lib/amm/position-utils";

interface SelectPriceRangeProps {
  pool: FormattedPool;
  initialLowerTick: number;
  initialUpperTick: number;
  onTicksChange: (lowerTick: number, upperTick: number) => void;
}

export function SelectPriceRange({
  pool,
  initialLowerTick,
  initialUpperTick,
  onTicksChange,
}: SelectPriceRangeProps) {
  const t = useTranslations();

  // Separate states for user input and displayed/calculated values
  const [minPriceInput, setMinPriceInput] = useState("");
  const [maxPriceInput, setMaxPriceInput] = useState("");

  // Refs to track previous input values to avoid unnecessary updates
  const prevMinInputRef = useRef("");
  const prevMaxInputRef = useRef("");

  // States for the actual ticks and formatted prices
  const [lowerTick, setLowerTick] = useState(initialLowerTick);
  const [upperTick, setUpperTick] = useState(initialUpperTick);

  // Initialize values when component mounts
  useEffect(() => {
    if (initialLowerTick && initialUpperTick) {
      setLowerTick(initialLowerTick);
      setUpperTick(initialUpperTick);

      const lowerPrice = TickMath.tickToPrice(initialLowerTick);
      const upperPrice = TickMath.tickToPrice(initialUpperTick);

      const formattedLower = formatDisplayPrice(lowerPrice);
      const formattedUpper = formatDisplayPrice(upperPrice);

      // Also set input values to match formatted values
      setMinPriceInput(formattedLower);
      setMaxPriceInput(formattedUpper);

      // Initialize previous values
      prevMinInputRef.current = formattedLower;
      prevMaxInputRef.current = formattedUpper;
    }
  }, [initialLowerTick, initialUpperTick]);

  // Calculate and update the lower price only when the input loses focus
  const processMinPrice = useCallback(
    (value: string) => {
      // Skip processing if the value hasn't actually changed
      if (value === prevMinInputRef.current) {
        return;
      }

      const numericValue = parseFloat(value.replace(/,/g, ""));
      if (!isNaN(numericValue) && numericValue > 0) {
        // Convert price to tick
        let newTick = TickMath.priceToTick(numericValue);
        // Round tick to spacing
        const roundedTick = TickMath.roundToTickSpacing(
          newTick,
          pool.tickSpacing,
        );

        // Only update if the tick is actually different than current lowerTick
        if (roundedTick !== lowerTick) {
          newTick = roundedTick;

          // If new tick is greater than or equal to upperTick, adjust upperTick
          if (newTick >= upperTick) {
            const newUpperTick = newTick + pool.tickSpacing;
            setUpperTick(newUpperTick);
            const newUpperPrice = TickMath.tickToPrice(newUpperTick);
            const formattedUpper = formatDisplayPrice(newUpperPrice);
            setMaxPriceInput(formattedUpper);
            prevMaxInputRef.current = formattedUpper;
            onTicksChange(newTick, newUpperTick);
          } else {
            setLowerTick(newTick);
            onTicksChange(newTick, upperTick);
          }

          // Update the formatted min price
          const lowerPrice = TickMath.tickToPrice(newTick);
          const formattedLower = formatDisplayPrice(lowerPrice);
          // Update input to show the correct value based on the tick
          setMinPriceInput(formattedLower);
          prevMinInputRef.current = formattedLower;
        } else {
          // If tick didn't change but input format might be different, update to consistent format
          const lowerPrice = TickMath.tickToPrice(lowerTick);
          const formattedLower = formatDisplayPrice(lowerPrice);
          if (formattedLower !== minPriceInput) {
            setMinPriceInput(formattedLower);
            prevMinInputRef.current = formattedLower;
          }
        }
      }
    },
    [pool.tickSpacing, upperTick, lowerTick, onTicksChange, minPriceInput],
  );

  // Calculate and update the upper price only when the input loses focus
  const processMaxPrice = useCallback(
    (value: string) => {
      // Skip processing if the value hasn't actually changed
      if (value === prevMaxInputRef.current) {
        return;
      }

      const numericValue = parseFloat(value.replace(/,/g, ""));
      if (!isNaN(numericValue) && numericValue > 0) {
        // Convert price to tick
        let newTick = TickMath.priceToTick(numericValue);
        // Round tick to spacing
        const roundedTick = TickMath.roundToTickSpacing(
          newTick,
          pool.tickSpacing,
        );

        // Only update if the tick is actually different than current upperTick
        if (roundedTick !== upperTick) {
          newTick = roundedTick;

          // If new tick is less than or equal to lowerTick, adjust lowerTick
          if (newTick <= lowerTick) {
            const newLowerTick = newTick - pool.tickSpacing;
            const adjustedLowerTick = Math.max(newLowerTick, TickMath.MIN_TICK);
            setLowerTick(adjustedLowerTick);
            const newLowerPrice = TickMath.tickToPrice(adjustedLowerTick);
            const formattedLower = formatDisplayPrice(newLowerPrice);
            setMinPriceInput(formattedLower);
            prevMinInputRef.current = formattedLower;
            onTicksChange(adjustedLowerTick, newTick);
          } else {
            setUpperTick(newTick);
            onTicksChange(lowerTick, newTick);
          }

          // Update the formatted max price
          const upperPrice = TickMath.tickToPrice(newTick);
          const formattedUpper = formatDisplayPrice(upperPrice);
          // Update input to show the correct value based on the tick
          setMaxPriceInput(formattedUpper);
          prevMaxInputRef.current = formattedUpper;
        } else {
          // If tick didn't change but input format might be different, update to consistent format
          const upperPrice = TickMath.tickToPrice(upperTick);
          const formattedUpper = formatDisplayPrice(upperPrice);
          if (formattedUpper !== maxPriceInput) {
            setMaxPriceInput(formattedUpper);
            prevMaxInputRef.current = formattedUpper;
          }
        }
      }
    },
    [pool.tickSpacing, lowerTick, upperTick, onTicksChange, maxPriceInput],
  );

  // Handle when user changes the minimum price input
  const handleMinPriceChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    // Just update the input value, don't process it yet
    setMinPriceInput(e.target.value);
  };

  // Handle when user changes the maximum price input
  const handleMaxPriceChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    // Just update the input value, don't process it yet
    setMaxPriceInput(e.target.value);
  };

  // Process input values only when focus is lost
  const handleBlur = (field: "min" | "max") => {
    if (field === "min") {
      processMinPrice(minPriceInput);
    } else if (field === "max") {
      processMaxPrice(maxPriceInput);
    }
  };

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-medium">{t("liquidity.priceRange")}</h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <label className="text-sm font-medium">
            {t("liquidity.minPrice")}
          </label>
          <input
            type="text"
            className="w-full p-2 border rounded-md"
            value={minPriceInput}
            onChange={handleMinPriceChange}
            onBlur={() => handleBlur("min")}
            placeholder="0.00"
          />
          <div className="flex justify-end">
            <p className="text-xs text-muted-foreground">Tick: {lowerTick}</p>
          </div>
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium">
            {t("liquidity.maxPrice")}
          </label>
          <input
            type="text"
            className="w-full p-2 border rounded-md"
            value={maxPriceInput}
            onChange={handleMaxPriceChange}
            onBlur={() => handleBlur("max")}
            placeholder="0.00"
          />
          <div className="flex justify-end">
            <p className="text-xs text-muted-foreground">Tick: {upperTick}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
