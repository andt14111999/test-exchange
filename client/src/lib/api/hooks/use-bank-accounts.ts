import { useQuery, useMutation } from "@tanstack/react-query";
import { queryClient } from "@/lib/query/query-client";
import {
  CreateBankAccountParams,
  UpdateBankAccountParams,
  getBankAccounts,
  getBankAccount,
  createBankAccount,
  updateBankAccount,
  deleteBankAccount,
  setPrimaryBankAccount,
} from "../bank-accounts";

/**
 * Hook to fetch all bank accounts
 */
export function useBankAccounts(params?: {
  country_code?: string;
  bank_name?: string;
  verified?: boolean;
  page?: number;
  per_page?: number;
}) {
  return useQuery({
    queryKey: ["bankAccounts", params],
    queryFn: () => getBankAccounts(params),
  });
}

/**
 * Hook to fetch a single bank account by ID
 */
export function useBankAccount(id: string) {
  return useQuery({
    queryKey: ["bankAccounts", id],
    queryFn: () => getBankAccount(id),
    enabled: !!id,
  });
}

/**
 * Hook to create a new bank account
 */
export function useCreateBankAccount() {
  return useMutation({
    mutationFn: (data: CreateBankAccountParams) => createBankAccount(data),
    onSuccess: () => {
      // Invalidate bank accounts queries to refetch the data
      queryClient.invalidateQueries({ queryKey: ["bankAccounts"] });
    },
  });
}

/**
 * Hook to update an existing bank account
 */
export function useUpdateBankAccount() {
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateBankAccountParams }) =>
      updateBankAccount(id, data),
    onSuccess: (_, variables) => {
      // Invalidate specific bank account query and the list
      queryClient.invalidateQueries({
        queryKey: ["bankAccounts", variables.id],
      });
      queryClient.invalidateQueries({ queryKey: ["bankAccounts"] });
    },
  });
}

/**
 * Hook to delete a bank account
 */
export function useDeleteBankAccount() {
  return useMutation({
    mutationFn: (id: string) => deleteBankAccount(id),
    onSuccess: () => {
      // Invalidate bank accounts queries to refetch the data
      queryClient.invalidateQueries({ queryKey: ["bankAccounts"] });
    },
  });
}

/**
 * Hook to set a bank account as primary
 */
export function useSetPrimaryBankAccount() {
  return useMutation({
    mutationFn: (id: string) => setPrimaryBankAccount(id),
    onSuccess: (_, id) => {
      // Invalidate specific bank account query and the list
      queryClient.invalidateQueries({ queryKey: ["bankAccounts", id] });
      queryClient.invalidateQueries({ queryKey: ["bankAccounts"] });
    },
  });
}
