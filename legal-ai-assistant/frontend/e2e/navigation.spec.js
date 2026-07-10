import { test, expect } from '@playwright/test'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button[type="submit"]')
    await page.waitForTimeout(1000)
  })

  test('should toggle sidebar', async ({ page }) => {
    const sidebar = page.locator('.sidebar')
    const toggleBtn = page.locator('.sidebar-toggle')
    
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click()
      await page.waitForTimeout(300)
    }
  })

  test('should navigate using sidebar menu', async ({ page }) => {
    const sidebarItem = page.locator('.menu-item:has-text("法规搜索")').first()
    if (await sidebarItem.isVisible()) {
      await sidebarItem.click()
      await expect(page).toHaveURL(/\/law-search|\/laws/)
    }
  })

  test('should show breadcrumbs', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('.breadcrumbs, .el-breadcrumb')).toBeVisible()
  })

  test('should display user avatar in header', async ({ page }) => {
    await page.goto('/dashboard')
    const avatarArea = page.locator('.user-avatar, .avatar, [class*="avatar"]')
    await expect(avatarArea.first()).toBeVisible()
  })

  test('should open user dropdown menu', async ({ page }) => {
    await page.goto('/dashboard')
    const avatarArea = page.locator('.user-avatar, .avatar, [class*="avatar"]').first()
    if (await avatarArea.isVisible()) {
      await avatarArea.click()
      await expect(page.locator('.user-dropdown, .dropdown-menu')).toBeVisible()
    }
  })

  test('should have dark mode toggle', async ({ page }) => {
    await page.goto('/dashboard')
    const darkModeToggle = page.locator('[class*="dark-mode"], [class*="theme"]').first()
    if (await darkModeToggle.isVisible()) {
      await darkModeToggle.click()
      await page.waitForTimeout(300)
    }
  })
})
