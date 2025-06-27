import { test as base, expect } from "@playwright/test";
import fs from "fs";
import path from "path";

// Mở rộng Playwright test với coverage thu thập từ client-side
export const test = base.extend({
  // Hook để thu thập JS coverage
  context: async ({ context }, runTest) => {
    // Tạo thư mục coverage nếu chưa tồn tại
    const coverageDir = path.join(process.cwd(), ".nyc_output");
    if (!fs.existsSync(coverageDir)) {
      fs.mkdirSync(coverageDir, { recursive: true });
    }

    // Hook vào các sự kiện tạo page mới để thu thập coverage
    context.on("page", async (page) => {
      // Khởi động thu thập coverage cho mỗi page
      if (page.coverage) {
        try {
          await page.coverage.startJSCoverage({
            resetOnNavigation: false,
          });
        } catch (e) {
          console.warn("Failed to start JS coverage:", e);
        }
      }
    });

    // Chạy test với context đã cấu hình
    await runTest(context);

    // Sau khi test hoàn thành, lưu coverage
    for (const page of context.pages()) {
      if (page.coverage) {
        try {
          const coverage = await page.coverage.stopJSCoverage();
          if (coverage && coverage.length > 0) {
            // Tạo tên file độc nhất cho mỗi test
            const testInfo = test.info();
            const sanitizedTitle = (testInfo.title || "unknown")
              .replace(/\s+/g, "-")
              .replace(/[^\w-]/g, "");
            const timestamp = Date.now();
            const filename = path.join(
              coverageDir,
              `client-coverage-${sanitizedTitle}-${timestamp}.json`,
            );

            // Lưu coverage data vào file
            fs.writeFileSync(filename, JSON.stringify(coverage));
          }
        } catch (e) {
          console.warn("Failed to collect JS coverage:", e);
        }
      }
    }
  },
});

export { expect };
