const { test, expect } = require('@playwright/test');

const credentials = {
  email: 'alexis@example.dev',
  password: 'demo1234',
};

async function login(page) {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  await page.getByRole('button', { name: 'Entrer' }).click();
  await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
  await expect(page.locator('#main-content')).toContainText('Explorer');
}

async function apiLogin(request) {
  const csrfResponse = await request.get('/api/csrf');
  expect(csrfResponse.ok()).toBeTruthy();
  const csrf = await csrfResponse.json();

  const loginResponse = await request.post('/api/auth/login', {
    headers: {
      [csrf.headerName]: csrf.token,
    },
    data: credentials,
  });
  expect(loginResponse.ok()).toBeTruthy();
  return csrf;
}

test.describe('Streamfolio e2e', () => {
  test('parcours principal : login, recherche, fiche, watchlist, lecteur, logout', async ({ page }) => {
    await login(page);

    await page.getByRole('searchbox', { name: 'Rechercher' }).fill('botanical');
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/#\/catalog\?query=botanical/);
    await expect(page.locator('#main-content')).toContainText('Botanical Cities');

    await page.getByRole('link', { name: /Afficher Botanical Cities/ }).first().click();
    await expect(page.getByRole('heading', { name: 'Botanical Cities' })).toBeVisible();

    await page.getByRole('button', { name: /Ma liste/ }).first().click();
    await page.getByRole('link', { name: 'Ma liste' }).first().click();
    await expect(page.locator('#main-content')).toContainText('Botanical Cities');

    await page.getByRole('button', { name: /Lire Botanical Cities|Lire/ }).first().click();
    await expect(page.locator('#video-player')).toBeVisible();
    await expect(page.locator('#player-mode')).toContainText(/MP4|HLS/);

    await page.getByLabel(/Se déconnecter/).click();
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  });

  test('les endpoints vidéo protégés refusent un utilisateur déconnecté', async ({ page }) => {
    await login(page);

    const authenticated = await page.request.get('/api/videos/1');
    expect(authenticated.status()).toBe(200);

    await page.getByLabel(/Se déconnecter/).click();
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();

    const anonymous = await page.request.get('/api/videos/1');
    expect(anonymous.status()).toBe(401);
  });

  test('le catalogue API est filtré et paginé côté serveur', async ({ request }) => {
    await apiLogin(request);

    const response = await request.get('/api/catalog?query=botanical&type=SERIES&genre=Botanique&page=0&size=2');
    expect(response.status()).toBe(200);
    const payload = await response.json();

    expect(payload.pagination.number).toBe(0);
    expect(payload.pagination.size).toBe(2);
    expect(payload.pagination.totalElements).toBeGreaterThanOrEqual(payload.items.length);
    expect(payload.items.length).toBeGreaterThan(0);
    expect(payload.items.length).toBeLessThanOrEqual(2);
    for (const item of payload.items) {
      expect(item.type).toBe('SERIES');
      expect(item.genres).toContain('Botanique');
    }
  });

  test('les mutations API sans CSRF sont refusées', async ({ request }) => {
    await apiLogin(request);

    const response = await request.put('/api/videos/1/progress', {
      data: {
        positionSeconds: 1,
        durationSeconds: 12,
      },
    });

    expect(response.status()).toBe(403);
  });
});
