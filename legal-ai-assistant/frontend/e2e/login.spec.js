import { test, expect } from '@playwright/test'

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('should display login form', async ({ page }) => {
    await expect(page.locator('input[type="text"]')).toBeVisible()
    await expect(page.locator('input[type="password"]')).toBeVisible()
    await expect(page.locator('button[type="submit"]')).toBeVisible()
  })

  test('should show register link', async ({ page }) => {
    await expect(page.locator('text=注册账号')).toBeVisible()
  })

  test('should navigate to register page', async ({ page }) => {
    await page.click('text=注册账号')
    await expect(page).toHaveURL(/\/register/)
  })

  test('should validate empty form submission', async ({ page }) => {
    await page.click('button[type="submit"]')
    await expect(page.locator('.el-message')).toBeVisible()
  })

  test('should have remember me checkbox', async ({ page }) => {
    await expect(page.locator('text=记住我')).toBeVisible()
  })
})
