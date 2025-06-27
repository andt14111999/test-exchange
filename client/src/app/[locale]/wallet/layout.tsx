export default function WalletLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-[calc(100vh-theme(spacing.14))] bg-gradient-to-b from-background to-muted">
      {children}
    </div>
  );
}
