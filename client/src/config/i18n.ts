import { getRequestConfig } from "next-intl/server";

export const locales = ["en", "vi", "fil"] as const;
export const defaultLocale = "en" as const;

export type Locale = (typeof locales)[number];

export default getRequestConfig(async ({ locale }) => {
  const resolvedLocale =
    !locale || !locales.includes(locale as Locale)
      ? defaultLocale
      : (locale as string);

  return {
    messages: (await import(`@/i18n/${resolvedLocale}.json`)).default,
    locale: resolvedLocale,
    timeZone: "Asia/Ho_Chi_Minh",
  };
});
