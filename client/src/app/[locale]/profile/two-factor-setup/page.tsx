import { TwoFactorAuthSetup } from "@/components/two-factor-auth-setup";
import { ProtectedLayout } from "@/components/protected-layout";

export default function TwoFactorSetupPage() {
  return (
    <ProtectedLayout>
      <div className="container py-8">
        <TwoFactorAuthSetup />
      </div>
    </ProtectedLayout>
  );
}
