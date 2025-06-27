"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TwoFactorAuthDisableDialog } from "@/components/two-factor-auth-disable-dialog";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";
import {
  Mail,
  Shield,
  User,
  AtSign,
  Award,
  PhoneCall,
  FileCheck,
} from "lucide-react";
import { useTranslations } from "next-intl";
import Image from "next/image";
import { useEffect, useState } from "react";

export default function ProfilePage() {
  const t = useTranslations();
  const router = useRouter();
  const { user } = useUserStore();
  const [showDisable2FADialog, setShowDisable2FADialog] = useState(false);

  // Redirect nếu user không có username
  useEffect(() => {
    if (user && !user.username) {
      router.push("/profile/update");
    }
  }, [user, router]);

  // Hàm để hiển thị màu dựa trên trạng thái
  const getStatusColor = (status?: string) => {
    if (!status) return "bg-gray-200";

    switch (status.toLowerCase()) {
      case "active":
        return "bg-green-100 text-green-800 border-green-300";
      case "suspended":
        return "bg-yellow-100 text-yellow-800 border-yellow-300";
      case "banned":
        return "bg-red-100 text-red-800 border-red-300";
      default:
        return "bg-gray-100 text-gray-800 border-gray-300";
    }
  };

  const handleSetup2FA = () => {
    router.push("/profile/two-factor-setup");
  };

  const handleDisable2FASuccess = () => {
    // Refresh user data để cập nhật trạng thái 2FA
    window.location.reload();
  };

  const loadingContent = (
    <div className="container py-8">
      <Card className="max-w-2xl mx-auto">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            <Skeleton className="h-6 w-24" />
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-4">
            <Skeleton className="h-20 w-20 rounded-full" />
            <div className="space-y-2">
              <Skeleton className="h-5 w-32" />
              <Skeleton className="h-4 w-48" />
            </div>
          </div>
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <AtSign className="h-4 w-4 text-muted-foreground" />
              <Skeleton className="h-4 w-40" />
            </div>
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-muted-foreground" />
              <Skeleton className="h-4 w-64" />
            </div>
            <div className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-muted-foreground" />
              <Skeleton className="h-4 w-32" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  const content = (
    <div className="container py-8">
      <Card className="max-w-2xl mx-auto">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            {t("profile.title")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-4">
            <div className="h-20 w-20 rounded-full bg-muted flex items-center justify-center">
              {user?.avatar ? (
                <Image
                  src={user.avatar}
                  alt={user.name || user.email}
                  width={80}
                  height={80}
                  className="rounded-full object-cover"
                />
              ) : (
                <User className="h-10 w-10 text-muted-foreground" />
              )}
            </div>
            <div>
              <h3 className="text-lg font-semibold">
                {user?.name || user?.email}
              </h3>
              <p className="text-sm text-muted-foreground">{user?.email}</p>
              {user?.status && (
                <div
                  className={`inline-block px-2 py-1 mt-1 text-xs font-medium rounded border ${getStatusColor(user.status)}`}
                >
                  {user.status}
                </div>
              )}
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <AtSign className="h-4 w-4 text-muted-foreground" />
              <span>
                {t("profile.username")}: {user?.username || t("profile.notSet")}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-muted-foreground" />
              <span>
                {t("profile.email")}: {user?.email}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-muted-foreground" />
              <span>
                {t("profile.role")}:{" "}
                {user?.role
                  ? t(`common.roles.${user.role.toLowerCase()}`)
                  : t("common.roles.user")}
              </span>
            </div>
            {user?.kycLevel !== undefined && (
              <div className="flex items-center gap-2">
                <Award className="h-4 w-4 text-muted-foreground" />
                <span>
                  {t("profile.kycLevel")}: {user.kycLevel}
                </span>
              </div>
            )}
            {user?.phoneVerified !== undefined && (
              <div className="flex items-center gap-2">
                <PhoneCall className="h-4 w-4 text-muted-foreground" />
                <span>
                  {t("profile.phoneVerified")}:{" "}
                  {user.phoneVerified ? t("common.yes") : t("common.no")}
                </span>
              </div>
            )}
            {user?.documentVerified !== undefined && (
              <div className="flex items-center gap-2">
                <FileCheck className="h-4 w-4 text-muted-foreground" />
                <span>
                  {t("profile.documentVerified")}:{" "}
                  {user.documentVerified ? t("common.yes") : t("common.no")}
                </span>
              </div>
            )}

            {/* 2FA Status */}
            <div className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-muted-foreground" />
              <span>
                Xác thực 2 bước:{" "}
                {user?.authenticatorEnabled ? (
                  <span className="inline-flex items-center gap-1">
                    <span className="text-green-600 font-medium">Đã bật</span>
                    <div className="h-2 w-2 bg-green-500 rounded-full"></div>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1">
                    <span className="text-red-600 font-medium">Chưa bật</span>
                    <div className="h-2 w-2 bg-red-500 rounded-full"></div>
                  </span>
                )}
              </span>
            </div>
          </div>

          <div className="pt-4 flex gap-2 flex-wrap">
            <Button
              variant="outline"
              onClick={() => router.push("/profile/edit")}
            >
              {t("profile.editProfile")}
            </Button>
            {!user?.username && (
              <Button onClick={() => router.push("/profile/update")}>
                {t("profile.setUsername")}
              </Button>
            )}

            {/* 2FA Action Buttons */}
            {user?.authenticatorEnabled ? (
              <Button
                variant="destructive"
                onClick={() => setShowDisable2FADialog(true)}
              >
                Tắt 2FA
              </Button>
            ) : (
              <Button
                onClick={handleSetup2FA}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <Shield className="h-4 w-4 mr-2" />
                Thiết lập 2FA
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      <TwoFactorAuthDisableDialog
        open={showDisable2FADialog}
        onOpenChange={setShowDisable2FADialog}
        onSuccess={handleDisable2FASuccess}
      />
    </div>
  );

  return (
    <ProtectedLayout loadingFallback={loadingContent}>
      {content}
    </ProtectedLayout>
  );
}
