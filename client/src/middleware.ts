import { defaultLocale, locales } from "@/config/i18n";
import createIntlMiddleware from "next-intl/middleware";
import { NextRequest } from "next/server";

const intlMiddleware = createIntlMiddleware({
  locales,
  defaultLocale,
  localePrefix: "as-needed",
});

export default function middleware(request: NextRequest) {
  return intlMiddleware(request);
}

export const config = {
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
