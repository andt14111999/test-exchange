import React from "react";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useBankAccounts,
  useBankAccount,
  useCreateBankAccount,
  useUpdateBankAccount,
  useDeleteBankAccount,
  useSetPrimaryBankAccount,
} from "../../../../../src/lib/api/hooks/use-bank-accounts";
import {
  getBankAccounts,
  getBankAccount,
  createBankAccount,
  updateBankAccount,
  deleteBankAccount,
  setPrimaryBankAccount,
  type BankAccount,
} from "../../../../../src/lib/api/bank-accounts";

// Mock the bank-accounts API module
jest.mock("../../../../../src/lib/api/bank-accounts");

// Create a wrapper with QueryClientProvider
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
        staleTime: Infinity,
      },
    },
  });

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = "QueryClientWrapper";
  return [Wrapper, queryClient] as const;
};

describe("Bank Account Hooks", () => {
  const mockBankAccount: BankAccount = {
    id: "123",
    bank_name: "Test Bank",
    account_name: "John Doe",
    account_number: "1234567890",
    branch: "Main Branch",
    country_code: "US",
    is_primary: true,
    verified: true,
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("useBankAccounts", () => {
    it("should fetch bank accounts without params", async () => {
      const mockResponse = { data: [mockBankAccount] };
      (getBankAccounts as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccounts(), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(true);
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(getBankAccounts).toHaveBeenCalledWith(undefined);
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should fetch bank accounts with params", async () => {
      const params = {
        country_code: "US",
        bank_name: "Test Bank",
        verified: true,
        page: 1,
        per_page: 10,
      };
      const mockResponse = { data: [mockBankAccount] };
      (getBankAccounts as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccounts(params), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(true);
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(getBankAccounts).toHaveBeenCalledWith(params);
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should handle error when fetching bank accounts fails", async () => {
      const error = new Error("Failed to fetch bank accounts");
      (getBankAccounts as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccounts(), {
        wrapper: Wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });

  describe("useBankAccount", () => {
    it("should fetch a single bank account", async () => {
      const mockResponse = { data: mockBankAccount };
      (getBankAccount as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccount("123"), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(true);
      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(getBankAccount).toHaveBeenCalledWith("123");
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should not fetch when id is not provided", () => {
      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccount(""), {
        wrapper: Wrapper,
      });

      expect(result.current.isLoading).toBe(false);
      expect(getBankAccount).not.toHaveBeenCalled();
    });

    it("should handle error when fetching a single bank account fails", async () => {
      const error = new Error("Failed to fetch bank account");
      (getBankAccount as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useBankAccount("123"), {
        wrapper: Wrapper,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });

  describe("useCreateBankAccount", () => {
    it("should create a bank account successfully", async () => {
      const mockResponse = { data: mockBankAccount };
      (createBankAccount as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useCreateBankAccount(), {
        wrapper: Wrapper,
      });

      const createData = {
        bank_name: "Test Bank",
        account_name: "John Doe",
        account_number: "1234567890",
        branch: "Main Branch",
        country_code: "US",
        is_primary: true,
      };

      result.current.mutate(createData);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(createBankAccount).toHaveBeenCalledWith(createData);
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should handle error when creating a bank account fails", async () => {
      const error = new Error("Failed to create bank account");
      (createBankAccount as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useCreateBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate({
        bank_name: "Test Bank",
        account_name: "John Doe",
        account_number: "1234567890",
        branch: "Main Branch",
        country_code: "US",
        is_primary: true,
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });

  describe("useUpdateBankAccount", () => {
    it("should update a bank account successfully", async () => {
      const mockResponse = { data: mockBankAccount };
      (updateBankAccount as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useUpdateBankAccount(), {
        wrapper: Wrapper,
      });

      const updateData = {
        bank_name: "Updated Bank",
        account_name: "Jane Doe",
      };

      result.current.mutate({ id: "123", data: updateData });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(updateBankAccount).toHaveBeenCalledWith("123", updateData);
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should handle error when updating a bank account fails", async () => {
      const error = new Error("Failed to update bank account");
      (updateBankAccount as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useUpdateBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate({
        id: "123",
        data: { bank_name: "Updated Bank" },
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });

  describe("useDeleteBankAccount", () => {
    it("should delete a bank account successfully", async () => {
      const mockResponse = { data: { success: true } };
      (deleteBankAccount as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useDeleteBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate("123");

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(deleteBankAccount).toHaveBeenCalledWith("123");
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should handle error when deleting a bank account fails", async () => {
      const error = new Error("Failed to delete bank account");
      (deleteBankAccount as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useDeleteBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate("123");

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });

  describe("useSetPrimaryBankAccount", () => {
    it("should set a bank account as primary successfully", async () => {
      const mockResponse = { data: { ...mockBankAccount, is_primary: true } };
      (setPrimaryBankAccount as jest.Mock).mockResolvedValueOnce(mockResponse);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useSetPrimaryBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate("123");

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(setPrimaryBankAccount).toHaveBeenCalledWith("123");
      expect(result.current.data).toEqual(mockResponse);
    });

    it("should handle error when setting primary bank account fails", async () => {
      const error = new Error("Failed to set primary bank account");
      (setPrimaryBankAccount as jest.Mock).mockRejectedValueOnce(error);

      const [Wrapper] = createWrapper();
      const { result } = renderHook(() => useSetPrimaryBankAccount(), {
        wrapper: Wrapper,
      });

      result.current.mutate("123");

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBe(error);
    });
  });
});
