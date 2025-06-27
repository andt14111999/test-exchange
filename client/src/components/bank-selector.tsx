"use client";

import { useState, useMemo } from "react";
import { useBanks } from "@/lib/api/hooks/use-banks";
import { Bank } from "@/lib/api/banks";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Check, ChevronsUpDown, Loader2, Search } from "lucide-react";
import { cn } from "@/lib/utils";
import Image from "next/image";
import { useTranslations } from "next-intl";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

interface BankSelectorProps {
  value: string;
  onChange: (value: string, bank: Bank | null) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

export function BankSelector({
  value,
  onChange,
  placeholder,
  className,
  disabled = false,
}: BankSelectorProps) {
  const t = useTranslations("bankAccounts");
  const [open, setOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  // Fetch banks data
  const { data: banksResponse, isLoading, error } = useBanks();
  const banks = useMemo(() => banksResponse?.data || [], [banksResponse?.data]);

  // Find the selected bank
  const selectedBank = useMemo(
    () => banks.find((bank) => String(bank.code) === String(value)) || null,
    [banks, value],
  );

  // Filter banks based on search term
  const filteredBanks = useMemo(() => {
    if (!searchTerm) return banks;
    const lowerSearchTerm = searchTerm.toLowerCase();
    return banks.filter(
      (bank) =>
        bank.name.toLowerCase().includes(lowerSearchTerm) ||
        bank.code.toLowerCase().includes(lowerSearchTerm) ||
        bank.shortName.toLowerCase().includes(lowerSearchTerm) ||
        bank.short_name.toLowerCase().includes(lowerSearchTerm),
    );
  }, [banks, searchTerm]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className={cn(
            "w-full justify-between",
            value ? "text-left font-normal" : "text-muted-foreground",
            className,
          )}
          disabled={disabled || isLoading}
        >
          {isLoading ? (
            <div className="flex items-center gap-2">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>{t("loading")}</span>
            </div>
          ) : selectedBank ? (
            <div className="flex items-center gap-2 overflow-hidden">
              {selectedBank.logo && (
                <div className="flex-shrink-0 h-5 w-5 relative">
                  <Image
                    src={selectedBank.logo}
                    alt={selectedBank.name}
                    fill
                    className="object-contain"
                  />
                </div>
              )}
              <span className="truncate">{selectedBank.name}</span>
            </div>
          ) : (
            <span>{placeholder || t("selectBank")}</span>
          )}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        className="p-0 w-[calc(100%-2px)] min-w-[300px] max-w-[500px] z-50"
        align="center"
        sideOffset={5}
        avoidCollisions={true}
        collisionPadding={20}
      >
        <div className="p-3">
          <div className="flex items-center border rounded-md px-3 mb-3">
            <Search className="h-4 w-4 mr-2 text-muted-foreground" />
            <Input
              placeholder={t("searchBank")}
              className="h-10 border-0 p-0 focus-visible:ring-0 focus-visible:ring-offset-0"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          {error ? (
            <div className="py-6 text-center text-sm text-muted-foreground">
              {t("fetchError")}
            </div>
          ) : filteredBanks.length === 0 ? (
            <div className="py-6 text-center text-sm text-muted-foreground">
              {t("noResults")}
            </div>
          ) : (
            <div className="max-h-[350px] overflow-y-auto pr-1">
              {filteredBanks.map((bank) => (
                <div
                  key={bank.code}
                  className={cn(
                    "flex items-center gap-2 rounded-md px-3 py-2.5 text-base cursor-pointer hover:bg-accent",
                    String(value) === String(bank.code) && "bg-accent",
                  )}
                  onClick={() => {
                    onChange(String(bank.code), bank);
                    setOpen(false);
                  }}
                >
                  {bank.logo && (
                    <div className="flex-shrink-0 h-6 w-6 relative">
                      <Image
                        src={bank.logo}
                        alt={bank.name}
                        fill
                        className="object-contain"
                      />
                    </div>
                  )}
                  <span className="truncate flex-1 font-medium">
                    {bank.name}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {bank.code}
                  </span>
                  {String(value) === String(bank.code) && (
                    <Check className="ml-auto h-5 w-5 flex-shrink-0" />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}
