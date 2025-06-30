import { apiClient } from "./client";

export interface BankAccount {
  id: string;
  bank_name: string;
  bank_code?: string;
  account_name: string;
  account_number: string;
  country_code: string;
  is_primary: boolean;
  verified: boolean;
  created_at: string;
  updated_at: string;
}

export interface CreateBankAccountParams {
  bank_name: string;
  bank_code?: string;
  account_name: string;
  account_number: string;
  country_code: string;
  is_primary?: boolean;
}

export interface UpdateBankAccountParams {
  bank_name?: string;
  bank_code?: string;
  account_name?: string;
  account_number?: string;
  country_code?: string;
  is_primary?: boolean;
}

export async function getBankAccounts(params?: {
  country_code?: string;
  bank_name?: string;
  verified?: boolean;
  page?: number;
  per_page?: number;
}) {
  return apiClient.get<BankAccount[]>("/bank_accounts", { params });
}

export async function getBankAccount(id: string) {
  return apiClient.get<BankAccount>(`/bank_accounts/${id}`);
}

export async function createBankAccount(data: CreateBankAccountParams) {
  return apiClient.post<BankAccount>("/bank_accounts", data);
}

export async function updateBankAccount(
  id: string,
  data: UpdateBankAccountParams,
) {
  return apiClient.put<BankAccount>(`/bank_accounts/${id}`, data);
}

export async function deleteBankAccount(id: string) {
  return apiClient.delete<{ success: boolean; message: string }>(
    `/bank_accounts/${id}`,
  );
}

export async function setPrimaryBankAccount(id: string) {
  return apiClient.put<BankAccount>(`/bank_accounts/${id}/primary`);
}
