const { test, expect } = require('@playwright/test');

async function login(page) {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  await page.getByRole('button', { name: 'Entrer' }).click();
  await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
}

async function openAdmin(page) {
  await login(page);
  await page.getByRole('link', { name: 'Admin' }).click();
  expect(page.url()).toContain('#/admin');
  await expect(page.locator('#admin-title')).toHaveText('Administration vidéo');
}

test.describe('Administration media', () => {
  test('affiche et filtre le panneau admin video', async ({ page }) => {
    await openAdmin(page);

    await expect(page.getByRole('link', { name: 'Upload' })).toBeVisible();
    await expect(page.locator('#main-content')).toContainText('Vidéos');

    await page.locator('[data-admin-filter] input[name=query]').fill('Aurora');
    await page.locator('[data-admin-filter]').getByRole('button', { name: 'Filtrer' }).click();

    expect(page.url()).toContain('#/admin?');
    expect(page.url()).toContain('query=Aurora');
    await expect(page.locator('#main-content')).toContainText('Aurora');
  });

  test('ouvre la page upload dediee', async ({ page }) => {
    await openAdmin(page);
    await page.getByRole('link', { name: 'Upload' }).click();

    expect(page.url()).toContain('#/admin/upload');
    await expect(page.getByRole('heading', { name: 'Upload vidéo' })).toBeVisible();

    const accept = await page.getByLabel('Fichier vidéo').getAttribute('accept');
    expect(accept).toContain('.mkv');
    expect(accept).toContain('.avi');
    expect(accept).toContain('.wmv');
    expect(accept).toContain('.webm');
    expect(accept).toContain('.swf');
    expect(accept).toContain('.m2ts');
    await expect(page.locator('[data-duration-status]')).toHaveText('00:00');
    await expect(page.locator('[data-thumbnail-time]')).toBeDisabled();
    await expect(page.locator('[data-capture-thumbnail]')).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Aide sur la tagline' })).toBeVisible();
  });
});
