import { cn, formatDate } from "@/lib/utils";

describe("Utils", () => {
  describe("cn function", () => {
    it("should merge class names correctly", () => {
      expect(cn("class1", "class2")).toBe("class1 class2");
      expect(cn("class1", { class2: true, class3: false })).toBe(
        "class1 class2",
      );
      expect(cn("p-4 bg-red", "mt-2", "hover:bg-blue")).toBe(
        "p-4 bg-red mt-2 hover:bg-blue",
      );
      expect(cn("btn-primary", ["px-4", "py-2"])).toBe("btn-primary px-4 py-2");
    });

    it("should handle conditional classes", () => {
      const isActive = true;
      const isDisabled = false;

      expect(
        cn("base-class", {
          "is-active": isActive,
          "is-disabled": isDisabled,
        }),
      ).toBe("base-class is-active");
    });

    it("should handle empty or falsy inputs", () => {
      expect(cn()).toBe("");
      expect(cn("", null, undefined, false)).toBe("");
      expect(cn("valid-class", "", null)).toBe("valid-class");
    });

    it("should properly merge tailwind classes", () => {
      expect(cn("p-4 px-2", "p-8")).toBe("p-8");
      expect(cn("text-sm text-gray-500", "text-blue-500")).toBe(
        "text-sm text-blue-500",
      );
    });
  });

  describe("formatDate function", () => {
    const testDate = new Date("2024-03-20T15:30:45");

    it("should format date with default format", () => {
      expect(formatDate(testDate)).toBe("Mar 20, 2024 15:30");
    });

    it("should format date with custom format", () => {
      expect(formatDate(testDate, "yyyy-MM-dd")).toBe("2024-03-20");
      expect(formatDate(testDate, "HH:mm:ss")).toBe("15:30:45");
      expect(formatDate(testDate, "MMMM d, yyyy")).toBe("March 20, 2024");
    });

    it("should handle different input types", () => {
      const timestamp = testDate.getTime();
      const isoString = testDate.toISOString();

      expect(formatDate(timestamp)).toBe("Mar 20, 2024 15:30");
      expect(formatDate(isoString)).toBe("Mar 20, 2024 15:30");
    });

    it("should handle edge cases", () => {
      const newYear = new Date("2024-01-01T00:00:00");
      expect(formatDate(newYear)).toBe("Jan 1, 2024 00:00");

      const lastDayOfYear = new Date("2024-12-31T23:59:59");
      expect(formatDate(lastDayOfYear)).toBe("Dec 31, 2024 23:59");
    });
  });
});
