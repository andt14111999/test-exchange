import getRequestConfigDefault, {
  locales,
  defaultLocale,
  Locale,
} from "@/config/i18n";

// Mock next-intl/server
jest.mock("next-intl/server", () => ({
  getRequestConfig: jest.fn((callback) => callback),
}));

// Mock dynamic imports for locale messages
jest.mock("@/i18n/en.json", () => ({ default: { test: "test" } }), {
  virtual: true,
});
jest.mock("@/i18n/vi.json", () => ({ default: { test: "test" } }), {
  virtual: true,
});
jest.mock("@/i18n/fil.json", () => ({ default: { test: "test" } }), {
  virtual: true,
});

describe("i18n configuration", () => {
  describe("locales", () => {
    it("should export supported locales", () => {
      expect(locales).toEqual(["en", "vi", "fil"]);
    });

    it("should export default locale", () => {
      expect(defaultLocale).toBe("en");
    });

    it("should have defaultLocale included in locales", () => {
      expect(locales).toContain(defaultLocale);
    });

    it("should have correct Locale type", () => {
      const testLocale: Locale = "en";
      expect(testLocale).toBe("en");
    });
  });

  describe("getRequestConfig", () => {
    it("should return config with valid locale", async () => {
      const config = await getRequestConfigDefault({
        locale: "en",
        requestLocale: Promise.resolve("en"),
      });
      expect(config).toEqual({
        messages: {
          default: { test: "test" },
        },
        locale: "en",
        timeZone: "Asia/Ho_Chi_Minh",
      });
    });

    it("should return config with default locale when locale is undefined", async () => {
      const config = await getRequestConfigDefault({
        locale: undefined,
        requestLocale: Promise.resolve(undefined),
      });
      expect(config).toEqual({
        messages: {
          default: { test: "test" },
        },
        locale: defaultLocale,
        timeZone: "Asia/Ho_Chi_Minh",
      });
    });

    it("should return config with default locale when locale is invalid", async () => {
      const config = await getRequestConfigDefault({
        locale: "invalid" as Locale,
        requestLocale: Promise.resolve("invalid" as Locale),
      });
      expect(config).toEqual({
        messages: {
          default: { test: "test" },
        },
        locale: defaultLocale,
        timeZone: "Asia/Ho_Chi_Minh",
      });
    });

    it("should return config for each supported locale", async () => {
      for (const locale of locales) {
        const config = await getRequestConfigDefault({
          locale,
          requestLocale: Promise.resolve(locale),
        });
        expect(config).toEqual({
          messages: {
            default: { test: "test" },
          },
          locale,
          timeZone: "Asia/Ho_Chi_Minh",
        });
      }
    });
  });
});
