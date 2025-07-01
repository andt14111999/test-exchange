import { BankAccountSelector } from "@/components/bank-account-selector";
import { BankAccount } from "@/lib/api/bank-accounts";
import {
  useBankAccounts,
  useCreateBankAccount,
  useDeleteBankAccount,
  useUpdateBankAccount,
} from "@/lib/api/hooks/use-bank-accounts";
import "@testing-library/jest-dom";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { FormProvider, useForm } from "react-hook-form";
import { toast } from "sonner";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => jest.fn((key) => key)),
}));

// Mock sonner so that toast methods are spies
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
    message: jest.fn(),
    promise: jest.fn(),
    custom: jest.fn(),
    dismiss: jest.fn(),
    info: jest.fn(),
    warning: jest.fn(),
    loading: jest.fn(),
  },
}));

// Mock API hooks
jest.mock("@/lib/api/hooks/use-bank-accounts", () => ({
  useBankAccounts: jest.fn(),
  useCreateBankAccount: jest.fn(),
  useUpdateBankAccount: jest.fn(),
  useDeleteBankAccount: jest.fn(),
}));

// Mock BankSelector component
jest.mock("@/components/bank-selector", () => ({
  BankSelector: jest.fn(({ value, onChange, placeholder }) => (
    <select
      data-testid="bank-selector"
      value={value}
      onChange={(e) => {
        const bankCode = e.target.value;
        let mockBank = null;
        if (bankCode === "FRESHBANK") {
          mockBank = { name: "Fresh Bank" };
        } else if (bankCode === "TESTBANK") {
          mockBank = { name: "Test Bank" };
        }
        onChange(bankCode, mockBank);
      }}
    >
      <option value="">{placeholder}</option>
      <option value="FRESHBANK">Fresh Bank</option>
      <option value="TESTBANK">Test Bank</option>
    </select>
  )),
}));

// Mock error-handler specifically for extractErrorMessage
jest.mock("@/lib/utils/error-handler", () => ({
  ...jest.requireActual("@/lib/utils/error-handler"), // Use real handleApiError
  extractErrorMessage: jest.fn((error: unknown) => {
    // But keep this mock for extractErrorMessage
    if (error instanceof Error && error.message) return error.message;
    return "An unexpected error occurred";
  }),
}));

// Mock ResizeObserver
global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
}));

// Create a new QueryClient for each test
const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

// Mock data
const mockBankAccounts: BankAccount[] = [
  {
    id: "1",
    bank_name: "Bank A",
    account_number: "123",
    account_name: "Account A",
    country_code: "VN",
    is_primary: true,
    verified: true,
    created_at: "2023-01-01T00:00:00Z",
    updated_at: "2023-01-01T00:00:00Z",
  },
  {
    id: "2",
    bank_name: "Bank B",
    account_number: "456",
    account_name: "Account B",
    country_code: "US",
    is_primary: false,
    verified: true,
    created_at: "2023-01-02T00:00:00Z",
    updated_at: "2023-01-02T00:00:00Z",
  },
];

// Helper component to wrap BankAccountSelector with FormProvider and QueryClientProvider
const TestWrapper = ({
  bankAccountSelectorProps,
  defaultValues = {},
}: {
  bankAccountSelectorProps: Omit<
    React.ComponentProps<typeof BankAccountSelector>,
    "control"
  >;
  defaultValues?: Record<string, string>;
}) => {
  const methods = useForm({ defaultValues });
  const queryClient = createTestQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <FormProvider {...methods}>
        <BankAccountSelector
          {...bankAccountSelectorProps}
          control={methods.control}
        />
      </FormProvider>
    </QueryClientProvider>
  );
};

describe("BankAccountSelector", () => {
  // Declare mutation spies at a higher scope
  let mockCreateMutateAsync: jest.Mock;
  let mockUpdateMutateAsync: jest.Mock;
  let mockDeleteMutateAsync: jest.Mock;

  const defaultToastOptions = {
    position: "top-right",
    duration: 3000,
    className: "shadow-lg border border-gray-200 dark:border-gray-800",
  };

  const deleteToastOptions = {
    ...defaultToastOptions,
    duration: 5000,
  };

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();

    // Reset all handlers
    jest.resetModules();

    // Initialize the spies with proper async behavior
    mockCreateMutateAsync = jest.fn().mockImplementation(async (data) => {
      return Promise.resolve({
        data: {
          ...data,
          id: "default-create-id-from-beforeEach",
          verified: true,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        },
      });
    });

    mockUpdateMutateAsync = jest
      .fn()
      .mockImplementation(async ({ id, data }) => {
        return Promise.resolve({
          data: {
            ...mockBankAccounts.find((acc) => acc.id === id),
            ...data,
          },
        });
      });

    mockDeleteMutateAsync = jest.fn().mockImplementation(async () => {
      return Promise.resolve({});
    });

    // Setup hook mocks with consistent return values
    (useBankAccounts as jest.Mock).mockReturnValue({
      data: { data: mockBankAccounts },
      isLoading: false,
      error: null,
      refetch: jest.fn(),
    });

    (useCreateBankAccount as jest.Mock).mockReturnValue({
      mutateAsync: mockCreateMutateAsync,
      isPending: false,
      isError: false,
      error: null,
    });

    (useUpdateBankAccount as jest.Mock).mockReturnValue({
      mutateAsync: mockUpdateMutateAsync,
      isPending: false,
      isError: false,
      error: null,
    });

    (useDeleteBankAccount as jest.Mock).mockReturnValue({
      mutateAsync: mockDeleteMutateAsync,
      isPending: false,
      isError: false,
      error: null,
    });

    // Mock toast functions
    (toast.success as jest.Mock).mockImplementation(
      (message: string, options: unknown) => {
        // Return the message and options for verification
        return { message, options };
      },
    );
    (toast.error as jest.Mock).mockImplementation(
      (message: string, options: unknown) => {
        // Return the message and options for verification
        return { message, options };
      },
    );
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test("renders loading state initially", () => {
    (useBankAccounts as jest.Mock).mockReturnValueOnce({
      data: null,
      isLoading: true,
      error: null,
    });
    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );
    // The select trigger is disabled when loading
    expect(screen.getByRole("combobox")).toBeDisabled();
  });

  test("renders with no accounts message if no bank accounts are fetched", () => {
    (useBankAccounts as jest.Mock).mockReturnValueOnce({
      data: { data: [] },
      isLoading: false,
      error: null,
    });
    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );
    expect(screen.getByText("noAccounts")).toBeInTheDocument();
  });

  test("renders with bank accounts and selects the primary one by default", async () => {
    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );
    await waitFor(() => {
      expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
      expect(screen.getByText("(Primary)")).toBeInTheDocument();
    });
  });

  test("renders with bank accounts and selects the first one if no primary account", async () => {
    const accountsWithoutPrimary = mockBankAccounts.map((acc) => ({
      ...acc,
      is_primary: false,
    }));
    (useBankAccounts as jest.Mock).mockReturnValueOnce({
      data: { data: accountsWithoutPrimary },
      isLoading: false,
      error: null,
    });
    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );
    await waitFor(() => {
      expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
    });
  });

  test("calls onAccountSelect when an account is selected", async () => {
    const mockOnAccountSelect = jest.fn();
    render(
      <TestWrapper
        bankAccountSelectorProps={{
          name: "bankAccountId",
          onAccountSelect: mockOnAccountSelect,
        }}
      />,
    );

    await waitFor(() => {
      // Primary account (Bank A) should be selected by default
      expect(mockOnAccountSelect).toHaveBeenCalledWith(
        mockBankAccounts.find((acc) => acc.is_primary),
      );
    });

    mockOnAccountSelect.mockClear(); // Clear previous calls due to default selection

    fireEvent.click(screen.getByRole("combobox")); // Open select

    // Wait for the option to appear and then click it
    const bankBOption = await screen.findByText(/Bank B - 456/);
    fireEvent.click(bankBOption);

    expect(mockOnAccountSelect).toHaveBeenCalledWith(mockBankAccounts[1]);
  });

  test("shows error message when bank accounts fetch fails", async () => {
    const fetchError = new Error("Failed to fetch");
    (useBankAccounts as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: fetchError,
      refetch: jest.fn(),
    });

    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );

    const errorContainer = await screen.findByText("Failed to fetch");
    expect(errorContainer).toBeInTheDocument();
  });

  test("has exactly 3 action buttons", () => {
    render(
      <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
    );

    // Check for Add button
    expect(screen.getByTitle("addNew")).toBeInTheDocument();
    // Check for Edit button
    expect(screen.getByTitle("edit")).toBeInTheDocument();
    // Check for Delete button
    expect(screen.getByTitle("delete")).toBeInTheDocument();

    // Ensure there are exactly 3 buttons
    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(3);
  });

  // Add New Account
  describe("Add New Bank Account", () => {
    test("opens add new account dialog, submits successfully, and updates selection", async () => {
      const mockOnAccountSelect = jest.fn();
      const newAccountId = "specific-new-id-for-test1";
      const newAccountDataForTest = {
        id: newAccountId,
        bank_name: "Fresh Bank",
        account_name: "Fresh Account",
        account_number: "987650000",
        country_code: "VN",
        is_primary: true,
        verified: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      mockCreateMutateAsync.mockResolvedValueOnce({
        data: newAccountDataForTest,
      });

      render(
        <TestWrapper
          bankAccountSelectorProps={{
            name: "bankAccountId",
            onAccountSelect: mockOnAccountSelect,
          }}
        />,
      );

      // Wait for initial selection and clear mock
      await waitFor(() => {
        expect(mockOnAccountSelect).toHaveBeenCalledWith(
          expect.objectContaining({ id: mockBankAccounts[0].id }),
        );
      });
      mockOnAccountSelect.mockClear();

      // Open dialog
      fireEvent.click(screen.getByTitle("addNew"));
      const dialog = await screen.findByRole("dialog", {
        name: "addNewAccount",
      });
      expect(dialog).toBeInTheDocument();

      // Set form data
      // Use country select - find by the specific id
      const countryTrigger = within(dialog).getByRole("combobox", {
        name: /country/i,
      });
      fireEvent.click(countryTrigger);

      // Wait for options to appear and click Vietnam
      await waitFor(() => {
        const vietnamOptions = screen.getAllByText("Vietnam");
        // Click the option that's actually clickable (inside SelectContent)
        const selectItem = vietnamOptions.find((option) =>
          option.closest('[role="option"]'),
        );
        fireEvent.click(selectItem || vietnamOptions[1]); // fallback to second one (div)
      });

      // Use bank selector
      const bankSelector = within(dialog).getByTestId("bank-selector");
      fireEvent.change(bankSelector, { target: { value: "FRESHBANK" } });

      fireEvent.change(
        within(dialog).getByPlaceholderText(/enterAccountName/i),
        {
          target: { value: newAccountDataForTest.account_name },
        },
      );
      fireEvent.change(
        within(dialog).getByPlaceholderText(/enterAccountNumber/i),
        {
          target: { value: newAccountDataForTest.account_number },
        },
      );

      fireEvent.click(within(dialog).getByText("setPrimaryAccount"));

      // Submit form
      fireEvent.click(screen.getByText("addAccount"));

      // Assert mutation call
      await waitFor(() => {
        expect(mockCreateMutateAsync).toHaveBeenCalledWith(
          expect.objectContaining({
            bank_name: "Fresh Bank",
            account_name: newAccountDataForTest.account_name,
            account_number: newAccountDataForTest.account_number,
            is_primary: newAccountDataForTest.is_primary,
            country_code: "VN",
          }),
        );
      });

      // Assert success toast
      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          "createSuccess",
          defaultToastOptions,
        );
      });

      // Assert dialog closed
      await waitFor(() => {
        expect(
          screen.queryByRole("dialog", { name: "addNewAccount" }),
        ).not.toBeInTheDocument();
      });
    });

    test("shows error message in add dialog on creation failure", async () => {
      const createErrorMessage = "Create operation failed";
      const createError = new Error(createErrorMessage);

      (useCreateBankAccount as jest.Mock).mockReturnValue({
        mutateAsync: jest.fn().mockRejectedValue(createError),
        isPending: false,
        isError: true,
        error: createError,
      });

      const { extractErrorMessage } = jest.requireMock(
        "@/lib/utils/error-handler",
      );
      (extractErrorMessage as jest.Mock).mockReturnValue(createErrorMessage);

      render(
        <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
      );

      // Open dialog and fill form
      fireEvent.click(screen.getByTitle("addNew"));
      const dialog = await screen.findByRole("dialog");
      expect(dialog).toBeInTheDocument();

      // Fill required fields using more specific selectors
      const countryTrigger = within(dialog).getByRole("combobox", {
        name: /country/i,
      });
      fireEvent.click(countryTrigger);

      // Wait for options to appear and click Vietnam
      await waitFor(() => {
        const vietnamOptions = screen.getAllByText("Vietnam");
        // Click the option that's actually clickable (inside SelectContent)
        const selectItem = vietnamOptions.find((option) =>
          option.closest('[role="option"]'),
        );
        fireEvent.click(selectItem || vietnamOptions[1]); // fallback to second one (div)
      });

      // Use bank selector
      const bankSelector = within(dialog).getByTestId("bank-selector");
      fireEvent.change(bankSelector, { target: { value: "TESTBANK" } });

      fireEvent.change(
        within(dialog).getByPlaceholderText(/enterAccountName/i),
        {
          target: { value: "Test Account" },
        },
      );
      fireEvent.change(
        within(dialog).getByPlaceholderText(/enterAccountNumber/i),
        {
          target: { value: "12345" },
        },
      );

      // Submit form
      fireEvent.click(screen.getByText("addAccount"));

      // Verify error toast was shown
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          createErrorMessage,
          defaultToastOptions,
        );
      });
    });

    test("closes add dialog on cancel", () => {
      render(
        <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
      );
      fireEvent.click(screen.getByTitle("addNew"));
      expect(screen.getByText("addNewAccount")).toBeInTheDocument();
      fireEvent.click(screen.getByText("cancel"));
      expect(screen.queryByText("addNewAccount")).not.toBeInTheDocument();
    });
  });

  // Edit Bank Account
  describe("Edit Bank Account", () => {
    test("edit button is disabled if no account is selected", () => {
      (useBankAccounts as jest.Mock).mockReturnValueOnce({
        data: { data: [] }, // No accounts
        isLoading: false,
        error: null,
      });
      render(
        <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
      );
      expect(screen.getByTitle("edit")).toBeDisabled();
    });

    test("opens edit dialog with selected account data, submits successfully", async () => {
      const updatedAccountData = {
        ...mockBankAccounts[0],
        bank_name: "Updated Bank A",
      };
      (useUpdateBankAccount as jest.Mock).mockReturnValueOnce({
        mutateAsync: jest.fn().mockResolvedValue({ data: updatedAccountData }),
        isPending: false,
      });
      const mockOnAccountSelect = jest.fn();

      render(
        <TestWrapper
          bankAccountSelectorProps={{
            name: "bankAccountId",
            onAccountSelect: mockOnAccountSelect,
          }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }} // Pre-select an account
        />,
      );

      // Wait for initial selection
      await waitFor(() => {
        expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle("edit"));
      expect(screen.getByText("editAccount")).toBeInTheDocument();

      // Get the submit button directly and click it
      fireEvent.click(screen.getByText("saveChanges"));

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          "updateSuccess",
          expect.any(Object),
        );
      });
      // Dialog should close
      expect(screen.queryByText("editAccount")).not.toBeInTheDocument();
    });

    test("shows error message in edit dialog on update failure", async () => {
      const updateErrorMessage = "Update operation failed";
      const updateError = new Error(updateErrorMessage);

      (useUpdateBankAccount as jest.Mock).mockReturnValue({
        mutateAsync: jest.fn().mockRejectedValue(updateError),
        isPending: false,
        isError: true,
        error: updateError,
      });

      const { extractErrorMessage } = jest.requireMock(
        "@/lib/utils/error-handler",
      );
      (extractErrorMessage as jest.Mock).mockReturnValue(updateErrorMessage);

      render(
        <TestWrapper
          bankAccountSelectorProps={{ name: "bankAccountId" }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }}
        />,
      );

      // Open edit dialog
      fireEvent.click(screen.getByTitle("edit"));
      const dialog = await screen.findByRole("dialog", { name: "editAccount" });
      expect(dialog).toBeInTheDocument();

      // Submit form without changes to trigger error
      fireEvent.click(screen.getByText("saveChanges"));

      // Wait for error handling
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          updateErrorMessage,
          defaultToastOptions,
        );
      });

      // Verify dialog stays open with error
      const dialogWithError = await screen.findByRole("dialog", {
        name: "editAccount",
      });
      expect(dialogWithError).toBeInTheDocument();

      // Verify error alert is shown
      const errorContainer = within(dialogWithError)
        .getByText("Error")
        .closest("div");
      expect(errorContainer).toHaveTextContent(updateErrorMessage);
    });

    test("closes edit dialog on cancel", () => {
      render(
        <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
      );
      fireEvent.click(screen.getByTitle("edit"));
      expect(screen.getByText("editAccount")).toBeInTheDocument();
      fireEvent.click(screen.getByText("cancel"));
      expect(screen.queryByText("editAccount")).not.toBeInTheDocument();
    });
  });

  // Delete Bank Account
  describe("Delete Bank Account", () => {
    test("delete button is disabled if no account is selected", () => {
      (useBankAccounts as jest.Mock).mockReturnValueOnce({
        data: { data: [] },
        isLoading: false,
        error: null,
      });
      render(
        <TestWrapper bankAccountSelectorProps={{ name: "bankAccountId" }} />,
      );
      expect(screen.getByTitle("delete")).toBeDisabled();
    });

    test("opens delete confirmation dialog, confirms delete successfully", async () => {
      render(
        <TestWrapper
          bankAccountSelectorProps={{ name: "bankAccountId" }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle("delete"));
      expect(screen.getByText("deleteAccount")).toBeInTheDocument();
      expect(screen.getByText("deleteConfirmation")).toBeInTheDocument();

      fireEvent.click(screen.getByRole("button", { name: "delete" }));

      await waitFor(() => {
        expect(useDeleteBankAccount().mutateAsync).toHaveBeenCalledWith(
          mockBankAccounts[0].id,
        );
        expect(toast.success).toHaveBeenCalledWith(
          "deleteSuccess",
          expect.any(Object),
        );
      });
      // Dialog should close
      expect(screen.queryByText("deleteAccount")).not.toBeInTheDocument();
    });

    test("shows error message in delete dialog on deletion failure", async () => {
      const deleteErrorMessage = "Delete operation failed";
      const deleteError = new Error(deleteErrorMessage);

      (useDeleteBankAccount as jest.Mock).mockReturnValue({
        mutateAsync: jest.fn().mockRejectedValue(deleteError),
        isPending: false,
        isError: true,
        error: deleteError,
      });

      const { extractErrorMessage } = jest.requireMock(
        "@/lib/utils/error-handler",
      );
      (extractErrorMessage as jest.Mock).mockReturnValue(deleteErrorMessage);

      render(
        <TestWrapper
          bankAccountSelectorProps={{ name: "bankAccountId" }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }}
        />,
      );

      // Open delete dialog
      fireEvent.click(screen.getByTitle("delete"));
      const dialog = await screen.findByRole("dialog", {
        name: "deleteAccount",
      });
      expect(dialog).toBeInTheDocument();

      // Confirm delete
      fireEvent.click(within(dialog).getByRole("button", { name: "delete" }));

      // Wait for error handling
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          deleteErrorMessage,
          deleteToastOptions,
        );
      });

      // Verify dialog stays open with error
      const dialogWithError = await screen.findByRole("dialog", {
        name: "deleteAccount",
      });
      expect(dialogWithError).toBeInTheDocument();

      // Verify error alert is shown
      const errorContainer = within(dialogWithError)
        .getByText("Error")
        .closest("div");
      expect(errorContainer).toHaveTextContent(deleteErrorMessage);
    });

    test("shows specific error for trying to delete the only bank account", async () => {
      const specificErrorMessage = "cannotDeleteOnlyAccount";
      const deleteError = new Error("Cannot delete the only bank account");

      (useDeleteBankAccount as jest.Mock).mockReturnValue({
        mutateAsync: jest.fn().mockRejectedValue(deleteError),
        isPending: false,
        isError: true,
        error: deleteError,
      });

      const { extractErrorMessage } = jest.requireMock(
        "@/lib/utils/error-handler",
      );
      (extractErrorMessage as jest.Mock).mockReturnValue(specificErrorMessage);

      render(
        <TestWrapper
          bankAccountSelectorProps={{ name: "bankAccountId" }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }}
        />,
      );

      // Wait for initial render
      await waitFor(() => {
        expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
      });

      // Open delete dialog
      fireEvent.click(screen.getByTitle("delete"));
      const dialog = await screen.findByRole("dialog", {
        name: "deleteAccount",
      });
      expect(dialog).toBeInTheDocument();

      // Confirm delete
      fireEvent.click(within(dialog).getByRole("button", { name: "delete" }));

      // Wait for error handling
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          specificErrorMessage,
          deleteToastOptions,
        );
      });

      // Verify dialog stays open with error
      const dialogWithError = await screen.findByRole("dialog", {
        name: "deleteAccount",
      });
      expect(dialogWithError).toBeInTheDocument();

      // Verify error alert is shown
      const errorContainer = within(dialogWithError)
        .getByText("Error")
        .closest("div");
      expect(errorContainer).toHaveTextContent(specificErrorMessage);
    });

    test("closes delete dialog on cancel", async () => {
      render(
        <TestWrapper
          bankAccountSelectorProps={{ name: "bankAccountId" }}
          defaultValues={{ bankAccountId: mockBankAccounts[0].id }}
        />,
      );
      await waitFor(() => {
        expect(screen.getByText(/Bank A - 123/)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle("delete"));
      expect(screen.getByText("deleteAccount")).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: "cancel" }));
      expect(screen.queryByText("deleteAccount")).not.toBeInTheDocument();
    });
  });

  test("handles currentValue prop correctly to select an account", async () => {
    // This test ensures that if `currentValue` (from RHF's watch or direct prop)
    // is set, the corresponding account is selected.
    render(
      <TestWrapper
        bankAccountSelectorProps={{ name: "bankAccountId" }}
        defaultValues={{ bankAccountId: mockBankAccounts[1].id }} // Simulate RHF having a value
      />,
    );

    await waitFor(() => {
      // Bank B should be selected as its id is '2'
      expect(screen.getByText(/Bank B - 456/)).toBeInTheDocument();
      expect(screen.queryByText("(Primary)")).not.toBeInTheDocument(); // Bank B is not primary
    });
  });
});
