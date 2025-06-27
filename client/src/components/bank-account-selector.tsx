"use client";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  FormDescription,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { BankAccount } from "@/lib/api/bank-accounts";
import {
  useBankAccounts,
  useCreateBankAccount,
  useDeleteBankAccount,
  useUpdateBankAccount,
} from "@/lib/api/hooks/use-bank-accounts";
import { extractErrorMessage, handleApiError } from "@/lib/utils/error-handler";
import { AlertCircle, Pencil, PlusCircle, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import {
  Control,
  useWatch,
  ControllerRenderProps,
  FieldValues,
} from "react-hook-form";
import { toast } from "sonner";
import { BankSelector } from "@/components/bank-selector";

// Simple Alert component for error messages
interface AlertProps {
  variant?: "default" | "destructive";
  children: React.ReactNode;
  className?: string;
}

const Alert = ({
  variant = "default",
  children,
  className = "",
}: AlertProps) => {
  return (
    <div
      className={`p-4 rounded-md border ${
        variant === "destructive"
          ? "bg-red-50 border-red-200 text-red-800"
          : "bg-blue-50 border-blue-200 text-blue-800"
      } ${className}`}
    >
      {children}
    </div>
  );
};

const AlertTitle = ({ children }: { children: React.ReactNode }) => (
  <h5 className="font-medium mb-1">{children}</h5>
);

const AlertDescription = ({ children }: { children: React.ReactNode }) => (
  <div className="text-sm">{children}</div>
);

// List of country codes and names
const countries = [
  { code: "VN", name: "Vietnam" },
  { code: "US", name: "United States" },
  { code: "SG", name: "Singapore" },
  { code: "JP", name: "Japan" },
  { code: "KR", name: "South Korea" },
];

interface BankAccountSelectorProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  control: Control<any, any>;
  name: string;
  description?: string;
  onAccountSelect?: (account: BankAccount) => void;
}

// Add this component definition before the BankAccountSelector component
interface BankAccountDisplayProps {
  account: BankAccount;
}

const BankAccountDisplay = ({ account }: BankAccountDisplayProps) => {
  return (
    <div className="flex items-center gap-2">
      <div>
        {account.bank_name} - {account.account_number}
        {account.is_primary && (
          <span className="ml-1 font-medium">(Primary)</span>
        )}
      </div>
    </div>
  );
};

export function BankAccountSelector({
  control,
  name,
  description,
  onAccountSelect,
}: BankAccountSelectorProps) {
  const t = useTranslations("bankAccounts");
  const [formOpen, setFormOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<BankAccount | null>(
    null,
  );
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    bank_name: "",
    bank_code: "",
    account_name: "",
    account_number: "",
    branch: "",
    country_code: "",
    is_primary: false,
  });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Use ref to store the current field value with proper type
  const fieldRef = useRef<ControllerRenderProps<FieldValues, string>>(null);

  // React Query hooks
  const {
    data: bankAccountsResponse,
    isLoading,
    error: bankAccountsError,
  } = useBankAccounts();
  const bankAccounts = useMemo(
    () => bankAccountsResponse?.data || [],
    [bankAccountsResponse?.data],
  );

  const currentValue = useWatch({ control, name });

  // Sync currentValue with selectedAccount whenever it changes
  useEffect(() => {
    if (currentValue && bankAccounts.length > 0) {
      const account = bankAccounts.find((acc) => acc.id === currentValue);
      if (
        account &&
        (!selectedAccount || selectedAccount.id !== currentValue)
      ) {
        setSelectedAccount(account);
      }
    }
  }, [currentValue, bankAccounts, selectedAccount]);

  // Handle empty bank accounts data more gracefully
  useEffect(() => {
    // If bank accounts are loaded and there's at least one account,
    // select the first one by default if nothing is selected yet
    if (
      !isLoading &&
      bankAccounts.length > 0 &&
      !currentValue &&
      !selectedAccount?.id
    ) {
      const primaryAccount = bankAccounts.find((acc) => acc.is_primary);
      const accountToSelect = primaryAccount || bankAccounts[0];

      // Set selected account in local state
      setSelectedAccount(accountToSelect);

      // Notify parent component if provided
      if (onAccountSelect && accountToSelect) {
        onAccountSelect(accountToSelect);
      }
    }
  }, [
    bankAccounts,
    isLoading,
    currentValue,
    onAccountSelect,
    selectedAccount?.id,
  ]);

  // Show API error message if fetching bank accounts fails
  if (bankAccountsError && !errorMessage) {
    const errorMsg = handleApiError(bankAccountsError, t("fetchError"));
    setErrorMessage(errorMsg);
  }

  const createBankAccountMutation = useCreateBankAccount();
  const updateBankAccountMutation = useUpdateBankAccount();
  const deleteBankAccountMutation = useDeleteBankAccount();

  // Function to update form value and trigger validation
  const updateFormValue = useCallback(
    (account: BankAccount) => {
      if (fieldRef.current) {
        // Ensure the ID is a string
        const accountId = String(account.id);

        // Update the form value
        fieldRef.current.onChange(accountId);

        // Set selected account
        setSelectedAccount(account);

        // Notify parent component
        if (onAccountSelect) {
          onAccountSelect(account);
        }
      }
    },
    [onAccountSelect],
  );

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    try {
      if (isEditing && selectedAccount) {
        // Handle editing existing account
        const response = await updateBankAccountMutation.mutateAsync({
          id: selectedAccount.id,
          data: formData,
        });

        if (response.data) {
          updateFormValue(response.data);
          toast.success(t("updateSuccess"), {
            position: "top-right",
            duration: 3000,
            className: "shadow-lg border border-gray-200 dark:border-gray-800",
          });
          setFormOpen(false);
          resetForm();
        }
      } else {
        // Handle creating new account
        const response = await createBankAccountMutation.mutateAsync({
          bank_name: formData.bank_name,
          account_name: formData.account_name,
          account_number: formData.account_number,
          branch: formData.branch,
          country_code: formData.country_code,
          is_primary: formData.is_primary,
        });

        if (response.data) {
          updateFormValue(response.data);
          toast.success(t("createSuccess"), {
            position: "top-right",
            duration: 3000,
            className: "shadow-lg border border-gray-200 dark:border-gray-800",
          });
          setFormOpen(false);
          resetForm();
        }
      }
    } catch (error) {
      const errorMsg = extractErrorMessage(error);
      setErrorMessage(errorMsg);
      toast.error(errorMsg, {
        position: "top-right",
        duration: 3000,
        className: "shadow-lg border border-gray-200 dark:border-gray-800",
      });
    }
  };

  const handleDelete = async () => {
    if (!selectedAccount) return;
    setErrorMessage(null);

    try {
      await deleteBankAccountMutation.mutateAsync(selectedAccount.id);
      toast.success(t("deleteSuccess"), {
        position: "top-right",
        duration: 5000,
        className: "shadow-lg border border-gray-200 dark:border-gray-800",
      });
      setIsDeleteDialogOpen(false);
    } catch (error) {
      // Use the new error handler utility and keep dialog open to show error
      const errorMsg = extractErrorMessage(error);

      // Special handling for the specific error message
      if (errorMsg.includes("only bank account")) {
        const localizedMsg =
          t("cannotDeleteOnlyAccount") ||
          "Cannot delete the only bank account. Please add another one first.";

        toast.error(localizedMsg, {
          position: "top-right",
          duration: 5000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
        setErrorMessage(localizedMsg);
      } else {
        toast.error(errorMsg, {
          position: "top-right",
          duration: 5000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
        setErrorMessage(errorMsg);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      bank_name: "",
      bank_code: "",
      account_name: "",
      account_number: "",
      branch: "",
      country_code: "",
      is_primary: false,
    });
    setIsEditing(false);
    setFormOpen(false);
    setErrorMessage(null);
  };

  return (
    <>
      <FormField
        control={control}
        name={name}
        render={({ field }) => {
          // Update field ref whenever it changes
          fieldRef.current = field;

          return (
            <FormItem>
              {description && <FormDescription>{description}</FormDescription>}
              <div className="flex flex-col sm:flex-row gap-2">
                <div className="flex-1 min-w-0">
                  <Select
                    value={field.value || selectedAccount?.id || ""}
                    onValueChange={(value) => {
                      const account =
                        bankAccounts.find((acc) => acc.id === value) || null;
                      if (account) {
                        updateFormValue(account);
                      }
                    }}
                    disabled={isLoading || bankAccounts.length === 0}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder={t("selectAccount")}>
                        {selectedAccount && (
                          <BankAccountDisplay account={selectedAccount} />
                        )}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {bankAccounts.map((account) => (
                        <SelectItem key={account.id} value={account.id}>
                          <BankAccountDisplay account={account} />
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex gap-2 justify-end sm:justify-start">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => {
                      resetForm();
                      setFormOpen(true);
                    }}
                    type="button"
                    className="h-10 w-10"
                    title={t("addNew")}
                  >
                    <PlusCircle className="h-4 w-4" />
                    <span className="sr-only">{t("addNew")}</span>
                  </Button>
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => {
                      if (selectedAccount) {
                        setIsEditing(true);
                        setFormData({
                          bank_name: selectedAccount.bank_name,
                          bank_code: selectedAccount.bank_code || "",
                          account_name: selectedAccount.account_name,
                          account_number: selectedAccount.account_number,
                          branch: selectedAccount.branch || "",
                          country_code: selectedAccount.country_code,
                          is_primary: selectedAccount.is_primary,
                        });
                        setFormOpen(true);
                      }
                    }}
                    type="button"
                    className="h-10 w-10"
                    disabled={!selectedAccount}
                    title={t("edit")}
                  >
                    <Pencil className="h-4 w-4" />
                    <span className="sr-only">{t("edit")}</span>
                  </Button>
                  <Button
                    variant="destructive"
                    size="icon"
                    onClick={() => setIsDeleteDialogOpen(true)}
                    type="button"
                    className="h-10 w-10"
                    disabled={!selectedAccount}
                    title={t("delete")}
                  >
                    <Trash2 className="h-4 w-4" />
                    <span className="sr-only">{t("delete")}</span>
                  </Button>
                </div>
              </div>
              {bankAccounts.length === 0 && !isLoading && (
                <div className="flex items-center text-yellow-500 text-sm mt-2">
                  <AlertCircle className="h-4 w-4 mr-1" />
                  {t("noAccounts")}
                </div>
              )}
              {errorMessage && (
                <div className="relative w-full rounded-lg border border-destructive/50 p-4 mt-2 text-destructive dark:border-destructive">
                  <AlertCircle className="h-4 w-4 absolute left-4 top-4" />
                  <div className="text-sm pl-7">{errorMessage}</div>
                </div>
              )}
              {/* Không hiển thị validation error ở đây, để form validation xử lý */}
              {/* Hide the original FormMessage since we're showing custom errors */}
              <div className="hidden">
                <FormMessage />
              </div>
            </FormItem>
          );
        }}
      />

      {/* Bank Account Form Dialog */}
      <Dialog open={formOpen} onOpenChange={setFormOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>
              {isEditing ? t("editAccount") : t("addNewAccount")}
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleFormSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="country_code">{t("country")}</Label>
              <Select
                value={formData.country_code}
                onValueChange={(value) => {
                  setFormData((prev) => ({
                    ...prev,
                    country_code: value,
                  }));
                }}
                required
              >
                <SelectTrigger id="country_code" aria-label={t("country")}>
                  <SelectValue placeholder={t("selectCountry")} />
                </SelectTrigger>
                <SelectContent>
                  {countries.map((country) => (
                    <SelectItem key={country.code} value={country.code}>
                      {country.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="bank_name">{t("bankName")}</Label>
              <BankSelector
                value={formData.bank_code || ""}
                onChange={(code, bank) => {
                  setFormData({
                    ...formData,
                    bank_code: code,
                    bank_name: bank?.name || "",
                  });
                }}
                placeholder={t("selectBank")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="account_name">{t("accountName")}</Label>
              <Input
                id="account_name"
                value={formData.account_name}
                onChange={(e) =>
                  setFormData({ ...formData, account_name: e.target.value })
                }
                placeholder={t("enterAccountName")}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="account_number">{t("accountNumber")}</Label>
              <Input
                id="account_number"
                value={formData.account_number}
                onChange={(e) =>
                  setFormData({ ...formData, account_number: e.target.value })
                }
                placeholder={t("enterAccountNumber")}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="branch">{t("branch")}</Label>
              <Input
                id="branch"
                value={formData.branch}
                onChange={(e) =>
                  setFormData({ ...formData, branch: e.target.value })
                }
                placeholder={t("enterBranch")}
              />
            </div>
            {!isEditing && (
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="is_primary"
                  checked={formData.is_primary}
                  onCheckedChange={(checked) =>
                    setFormData({ ...formData, is_primary: checked as boolean })
                  }
                />
                <Label
                  htmlFor="is_primary"
                  className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                >
                  {t("setPrimaryAccount")}
                </Label>
              </div>
            )}
            {errorMessage && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Error</AlertTitle>
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
            )}
            <div className="flex justify-end space-x-2 pt-4">
              <DialogClose asChild>
                <Button variant="outline" type="button">
                  {t("cancel")}
                </Button>
              </DialogClose>
              <Button
                type="submit"
                disabled={
                  createBankAccountMutation.isPending ||
                  updateBankAccountMutation.isPending
                }
              >
                {isEditing ? t("saveChanges") : t("addAccount")}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>{t("deleteAccount")}</DialogTitle>
            <DialogDescription>{t("deleteConfirmation")}</DialogDescription>
          </DialogHeader>
          {errorMessage && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDeleteDialogOpen(false)}
              disabled={deleteBankAccountMutation.isPending}
            >
              {t("cancel")}
            </Button>
            <Button
              variant="destructive"
              onClick={handleDelete}
              disabled={deleteBankAccountMutation.isPending}
            >
              {deleteBankAccountMutation.isPending
                ? "Deleting..."
                : t("delete")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
