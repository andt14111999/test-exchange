import { NextIntlClientProvider } from "next-intl";
import { Header } from "@/components/layout/header";
import { ClientPoolInitializer } from "@/components/providers/client-pool-initializer";
import { QueryProvider } from "@/components/providers/query-provider";
import { NumberFormatProvider } from "@/components/providers/number-format-provider";
import { NotificationProvider } from "@/components/providers/notification-provider";
import { BalanceProvider } from "@/components/providers/balance-provider";
import { getMessages } from "next-intl/server";
import { locales, defaultLocale, type Locale } from "@/config/i18n";
import { Toast } from "@/components/ui/toast";

type Props = {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;

  // Sử dụng defaultLocale nếu locale không hợp lệ, thay vì gọi notFound()
  const resolvedLocale =
    !locale || !locales.includes(locale as Locale) ? defaultLocale : locale;

  const messages = await getMessages({ locale: resolvedLocale });

  return (
    <NextIntlClientProvider messages={messages} locale={resolvedLocale}>
      <NumberFormatProvider>
        <QueryProvider>
          <ClientPoolInitializer />
          <NotificationProvider>
            <BalanceProvider>
              <div className="relative min-h-screen flex flex-col">
                <Header />
                <main className="flex-1">
                  <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6">
                    {children}
                  </div>
                </main>
              </div>
            </BalanceProvider>
          </NotificationProvider>
        </QueryProvider>
      </NumberFormatProvider>
      <Toast />
    </NextIntlClientProvider>
  );
}
