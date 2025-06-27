import HomePage from "@/app/page";
import { redirect } from "next/navigation";
import { defaultLocale } from "@/config/i18n";

// Mock the redirect function from next/navigation
jest.mock("next/navigation", () => ({
  redirect: jest.fn(),
}));

// Mock the defaultLocale from config
jest.mock("@/config/i18n", () => ({
  defaultLocale: "en",
}));

describe("HomePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should redirect to the default locale", () => {
    // Call the HomePage component
    HomePage();

    // Check if redirect was called with the correct path
    expect(redirect).toHaveBeenCalledTimes(1);
    expect(redirect).toHaveBeenCalledWith(`/${defaultLocale}`);
  });
});
