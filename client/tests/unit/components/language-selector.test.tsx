import { render, screen, fireEvent } from "@testing-library/react";
import { LanguageSelector } from "@/components/language-selector";
import { useLocale, useTranslations } from "next-intl";
import { useRouter, usePathname } from "@/navigation";

// Mock the next-intl hooks
jest.mock("next-intl", () => ({
  useLocale: jest.fn(),
  useTranslations: jest.fn(),
}));

// Mock the navigation hooks
jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
  usePathname: jest.fn(),
}));

describe("LanguageSelector", () => {
  const mockRouter = {
    replace: jest.fn(),
  };
  const mockPathname = "/test-path";
  const mockTranslate = jest.fn((key) => key);

  beforeEach(() => {
    jest.clearAllMocks();

    // Setup default mocks
    (useLocale as jest.Mock).mockReturnValue("en");
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (usePathname as jest.Mock).mockReturnValue(mockPathname);
    (useTranslations as jest.Mock).mockReturnValue(mockTranslate);
  });

  it("renders with default language", () => {
    render(<LanguageSelector />);
    expect(screen.getByRole("combobox")).toBeInTheDocument();
    // The initial value should be "English" since locale is "en"
    expect(screen.getByText("English")).toBeInTheDocument();
  });

  it("displays all language options", async () => {
    render(<LanguageSelector />);

    // Open the select dropdown
    fireEvent.click(screen.getByRole("combobox"));

    // Check if all language options are present using more specific selectors
    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(3);
    expect(options[0]).toHaveTextContent("English");
    expect(options[1]).toHaveTextContent("Tiếng Việt");
    expect(options[2]).toHaveTextContent("Filipino");
  });

  it("changes language when a new option is selected", () => {
    render(<LanguageSelector />);

    // Open the select dropdown
    fireEvent.click(screen.getByRole("combobox"));

    // Select Vietnamese language
    fireEvent.click(screen.getByRole("option", { name: "Tiếng Việt" }));

    // Verify router.replace was called with correct arguments
    expect(mockRouter.replace).toHaveBeenCalledWith(mockPathname, {
      locale: "vi",
    });
  });

  it("uses current locale as default value", () => {
    (useLocale as jest.Mock).mockReturnValue("vi");

    render(<LanguageSelector />);

    // Check if the button shows the correct language name
    expect(screen.getByRole("combobox")).toHaveTextContent("Tiếng Việt");
  });

  it("translates placeholder text", () => {
    const mockLanguageText = "Select Language";
    const mockTranslateWithNamespace = jest.fn(() => mockLanguageText);
    (useTranslations as jest.Mock).mockReturnValue(mockTranslateWithNamespace);

    render(<LanguageSelector />);

    // The translation hook should be called with "common" namespace
    expect(useTranslations).toHaveBeenCalledWith("common");
    // The translation function should be called with "language" key
    expect(mockTranslateWithNamespace).toHaveBeenCalledWith("language");
  });
});
