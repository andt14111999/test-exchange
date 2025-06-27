import { ProtectedLayout } from "@/components/protected-layout";
import { AmmPoolProvider } from "@/providers/amm-pool-provider";

export default function LiquidityLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedLayout>
      <div className="min-h-[calc(100vh-theme(spacing.14))] bg-gradient-to-b from-background to-muted">
        <AmmPoolProvider>{children}</AmmPoolProvider>
      </div>
    </ProtectedLayout>
  );
}
