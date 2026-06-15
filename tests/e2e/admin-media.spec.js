const { test, expect } = require('@playwright/test');

async function login(page) {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  await page.getByRole('button', { name: 'Entrer' }).click();
  await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
}

test.describe('Administration media', () => {
  test('affiche et filtre le panneau admin video', async ({ page }) => {
    await login(page);

    await page.goto('/#/admin');
    await expect(page.getByRole('heading', { name: 'Administration vidéo' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Uploader une nouvelle vidéo' })).toBeVisible();
    await expect(page.locator('#main-content')).toContainText('Vidéos');

    await page.getByRole('searchbox', { name: 'Recherche' }).fill('Aurora');
    await page.getByRole('button', { name: 'Filtrer' }).click();

    await expect(page).toHaveURL(/#\/admin\?.*query=Aurora/);
    await expect(page.locator('#main-content')).toContainText('Aurora');
  });
});
