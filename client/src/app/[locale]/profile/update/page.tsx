"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { updateUsername } from "@/lib/api/user";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { AtSign } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

const formSchema = z.object({
  username: z
    .string()
    .min(3, { message: "Username must be at least 3 characters" })
    .max(50, { message: "Username must be less than 50 characters" })
    .regex(/^[a-zA-Z0-9_]+$/, {
      message: "Username can only contain letters, numbers, and underscores",
    }),
});

export default function UpdateUsernamePage() {
  const t = useTranslations();
  const router = useRouter();
  const { user, updateUsername: updateUserInStore } = useUserStore();
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Kiểm tra nếu đã có username
  useEffect(() => {
    if (user?.username) {
      // Nếu đã có username, quay lại trang profile
      router.push("/profile");
    }
  }, [user, router]);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: user?.username || "",
    },
  });

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true);
      const userData = await updateUsername(values.username);
      console.log("API response after username update:", userData);

      // Cập nhật store với username mới
      updateUserInStore(values.username);
      toast.success(t("profile.usernameUpdated"));

      // Chuyển hướng sau khi cập nhật thành công
      setTimeout(() => {
        router.push("/profile");
      }, 500); // Tăng thời gian chờ để đảm bảo state được cập nhật
    } catch (error) {
      console.error("Failed to update username:", error);
      toast.error(
        error instanceof Error
          ? error.message
          : t("profile.usernameUpdateFailed"),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <ProtectedLayout>
      <div className="container py-8">
        <Card className="max-w-md mx-auto">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AtSign className="h-5 w-5" />
              {t("profile.updateUsername")}
            </CardTitle>
            <CardDescription>
              {t("profile.updateUsernameDescription")}
            </CardDescription>
          </CardHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)}>
              <CardContent className="space-y-4">
                <FormField
                  control={form.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t("profile.username")}</FormLabel>
                      <FormControl>
                        <Input
                          placeholder={t("profile.usernamePlaceholder")}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </CardContent>
              <CardFooter className="flex justify-between">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.push("/profile")}
                >
                  {t("common.cancel")}
                </Button>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting
                    ? t("common.submitting")
                    : t("profile.saveUsername")}
                </Button>
              </CardFooter>
            </form>
          </Form>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
