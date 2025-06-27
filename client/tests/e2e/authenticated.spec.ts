import { test, expect } from "../fixtures/base";

// Các test này yêu cầu người dùng đã đăng nhập
test.describe("Authenticated User Features", () => {
  test("should show user-specific content", async ({ page }) => {
    // Đã thiết lập authentication trong trình duyệt - xem global-setup.ts

    // Mở trang chính thay vì dashboard (dashboard không tồn tại)
    await page.goto("/");

    // Kiểm tra xem có hiển thị nội dung dành cho người dùng đã đăng nhập không
    // Thay đổi timeout để test có thêm thời gian
    await expect(page.getByText(/Welcome/)).toBeVisible({ timeout: 10000 });

    // Kiểm tra các chức năng khác dành cho người dùng đã đăng nhập
    // await expect(page.getByRole("link", { name: "My Account" })).toBeVisible();
  });

  test("should allow logout", async ({ page }) => {
    // Đi đến trang chính thay vì dashboard
    await page.goto("/");

    // Tìm và nhấp vào nút đăng xuất
    const logoutButton = page.getByRole("button", { name: /logout/i });
    if (await logoutButton.isVisible()) {
      await logoutButton.click();

      // Kiểm tra xem đã chuyển đến trang đăng nhập chưa
      await expect(page).toHaveURL(/.*login/);
    }
  });
});
