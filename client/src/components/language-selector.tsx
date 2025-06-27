"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter, usePathname } from "@/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const languages = [
  { code: "en", name: "English" },
  { code: "vi", name: "Tiếng Việt" },
  { code: "fil", name: "Filipino" },
];

export function LanguageSelector() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();
  const t = useTranslations("common");

  const handleLanguageChange = (newLocale: string) => {
    router.replace(pathname, { locale: newLocale });
  };

  return (
    <Select defaultValue={locale} onValueChange={handleLanguageChange}>
      <SelectTrigger className="w-[120px]">
        <SelectValue placeholder={t("language")} />
      </SelectTrigger>
      <SelectContent>
        {languages.map((lang) => (
          <SelectItem key={lang.code} value={lang.code}>
            {lang.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
