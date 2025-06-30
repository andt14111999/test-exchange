import { apiClient } from "@/lib/api/client";
import {
  getBankAccounts,
  getBankAccount,
  createBankAccount,
  updateBankAccount,
  deleteBankAccount,
  setPrimaryBankAccount,
  type BankAccount,
  type CreateBankAccountParams,
  type UpdateBankAccountParams,
} from "@/lib/api/bank-accounts";

// Mock the API client
jest.mock("@/lib/api/client", () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe("Bank Accounts API", () => {
  const mockBankAccount: BankAccount = {
    id: "123",
    bank_name: "Test Bank",
    account_name: "John Doe",
    account_number: "1234567890",
    country_code: "US",
    is_primary: true,
    verified: true,
    created_at: "2024-03-20T00:00:00Z",
    updated_at: "2024-03-20T00:00:00Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getBankAccounts", () => {
    it("should fetch bank accounts without params successfully", async () => {
      const mockResponse = { data: [mockBankAccount] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getBankAccounts();

      expect(apiClient.get).toHaveBeenCalledWith("/bank_accounts", {
        params: undefined,
      });
      expect(result).toEqual(mockResponse);
    });

    it("should fetch bank accounts with params successfully", async () => {
      const params = {
        country_code: "US",
        bank_name: "Test Bank",
        verified: true,
        page: 1,
        per_page: 10,
      };
      const mockResponse = { data: [mockBankAccount] };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getBankAccounts(params);

      expect(apiClient.get).toHaveBeenCalledWith("/bank_accounts", { params });
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when fetching bank accounts fails", async () => {
      const mockError = new Error("Failed to fetch bank accounts");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getBankAccounts()).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("getBankAccount", () => {
    it("should fetch a single bank account successfully", async () => {
      const mockResponse = { data: mockBankAccount };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await getBankAccount("123");

      expect(apiClient.get).toHaveBeenCalledWith("/bank_accounts/123");
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when fetching a single bank account fails", async () => {
      const mockError = new Error("Failed to fetch bank account");
      (apiClient.get as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(getBankAccount("123")).rejects.toThrow(mockError);
      expect(apiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("createBankAccount", () => {
    const createParams: CreateBankAccountParams = {
      bank_name: "Test Bank",
      account_name: "John Doe",
      account_number: "1234567890",
      country_code: "US",
      is_primary: true,
    };

    it("should create a bank account successfully", async () => {
      const mockResponse = { data: mockBankAccount };
      (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await createBankAccount(createParams);

      expect(apiClient.post).toHaveBeenCalledWith(
        "/bank_accounts",
        createParams,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when creating a bank account fails", async () => {
      const mockError = new Error("Failed to create bank account");
      (apiClient.post as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(createBankAccount(createParams)).rejects.toThrow(mockError);
      expect(apiClient.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("updateBankAccount", () => {
    const updateParams: UpdateBankAccountParams = {
      bank_name: "Updated Bank",
      account_name: "Jane Doe",
    };

    it("should update a bank account successfully", async () => {
      const mockResponse = { data: { ...mockBankAccount, ...updateParams } };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await updateBankAccount("123", updateParams);

      expect(apiClient.put).toHaveBeenCalledWith(
        "/bank_accounts/123",
        updateParams,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when updating a bank account fails", async () => {
      const mockError = new Error("Failed to update bank account");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(updateBankAccount("123", updateParams)).rejects.toThrow(
        mockError,
      );
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });

  describe("deleteBankAccount", () => {
    it("should delete a bank account successfully", async () => {
      const mockResponse = {
        data: { success: true, message: "Bank account deleted" },
      };
      (apiClient.delete as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await deleteBankAccount("123");

      expect(apiClient.delete).toHaveBeenCalledWith("/bank_accounts/123");
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when deleting a bank account fails", async () => {
      const mockError = new Error("Failed to delete bank account");
      (apiClient.delete as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(deleteBankAccount("123")).rejects.toThrow(mockError);
      expect(apiClient.delete).toHaveBeenCalledTimes(1);
    });
  });

  describe("setPrimaryBankAccount", () => {
    it("should set a bank account as primary successfully", async () => {
      const mockResponse = { data: { ...mockBankAccount, is_primary: true } };
      (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

      const result = await setPrimaryBankAccount("123");

      expect(apiClient.put).toHaveBeenCalledWith("/bank_accounts/123/primary");
      expect(result).toEqual(mockResponse);
    });

    it("should handle error when setting primary bank account fails", async () => {
      const mockError = new Error("Failed to set primary bank account");
      (apiClient.put as jest.Mock).mockRejectedValueOnce(mockError);

      await expect(setPrimaryBankAccount("123")).rejects.toThrow(mockError);
      expect(apiClient.put).toHaveBeenCalledTimes(1);
    });
  });
});
