"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";
import { registerAsMerchant } from "@/lib/api/merchant";
import { useUserStore } from "@/lib/store/user-store";
import { zodResolver } from "@hookform/resolvers/zod";
import { AxiosError } from "axios";
import { AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import * as z from "zod";

const formSchema = z.object({
  termsAccepted: z.boolean().refine((val) => val === true, {
    message: "You must accept the terms and conditions",
  }),
});

type FormValues = z.infer<typeof formSchema>;

export default function MerchantRegistration() {
  const router = useRouter();
  const t = useTranslations("merchant.register");
  const { setUser, user } = useUserStore();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      termsAccepted: false,
    },
  });

  async function onSubmit() {
    try {
      setErrorMessage(null);
      const response = await registerAsMerchant();

      // Update user role in the store
      if (response.data && user) {
        setUser({
          ...user,
          role: response.data.role,
        });
      }

      toast.success(t("success"));
      router.push("/merchant/mint-fiat");
    } catch (error) {
      console.error("Failed to register as merchant:", error);

      // Extract error message from API response
      if (error instanceof AxiosError && error.response?.data?.message) {
        setErrorMessage(error.response.data.message);
      } else {
        setErrorMessage(t("error"));
      }

      toast.error(t("error"));
    }
  }

  const content = (
    <div className="container mx-auto max-w-2xl py-8">
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
        </CardHeader>
        <CardContent>
          {errorMessage && (
            <div className="relative w-full rounded-lg border border-destructive/50 p-4 text-destructive dark:border-destructive mb-6">
              <AlertCircle className="h-4 w-4 absolute left-4 top-4" />
              <h5 className="mb-1 font-medium leading-none tracking-tight pl-7">
                {t("error")}
              </h5>
              <div data-testid="error-message" className="text-sm pl-7">
                {errorMessage}
              </div>
            </div>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <div className="space-y-4">
                <h3 className="text-lg font-medium">{t("terms.title")}</h3>
                <div className="rounded-md border p-4 text-sm">
                  <p className="mb-2">{t("terms.content")}</p>
                  <ul className="list-disc pl-5 space-y-1">
                    <li>{t("terms.rule1")}</li>
                    <li>{t("terms.rule2")}</li>
                    <li>{t("terms.rule3")}</li>
                    <li>{t("terms.rule4")}</li>
                  </ul>
                </div>
              </div>

              <FormField
                control={form.control}
                name="termsAccepted"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <div className="space-y-1 leading-none">
                      <p>{t("terms.accept")}</p>
                      <FormMessage />
                    </div>
                  </FormItem>
                )}
              />

              <Button type="submit" className="w-full">
                {t("submit")}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );

  return <ProtectedLayout>{content}</ProtectedLayout>;
}
