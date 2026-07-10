import { test, expect } from '@playwright/test'

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button[type="submit"]')
    await page.waitForURL(/\/dashboard|\//)
  })

  test('should display greeting', async ({ page }) => {
    await expect(page.locator('.greeting-text')).toBeVisible()
  })

  test('should display stats cards', async ({ page }) => {
    await expect(page.locator('.stat-card').first()).toBeVisible()
  })

  test('should display quick access section', async ({ page }) => {
    await expect(page.locator('.quick-access')).toBeVisible()
  })

  test('should navigate to legal search', async ({ page }) => {
    await page.click('.quick-item:has-text("AI搜法")')
    await expect(page).toHaveURL(/\/legal-search/)
  })

  test('should navigate to case search', async ({ page }) => {
    await page.click('.quick-item:has-text("案例搜索")')
    await expect(page).toHaveURL(/\/case-search/)
  })

  test('should display recent activity', async ({ page }) => {
    await expect(page.locator('.recent-activity')).toBeVisible()
  })

  test('should display hot topics', async ({ page }) => {
    await expect(page.locator('.hot-topics')).toBeVisible()
  })
})
