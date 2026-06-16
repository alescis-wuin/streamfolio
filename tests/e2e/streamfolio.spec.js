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

async function openTitleDetail(page, slug, title) {
  await page.goto(`/#/title/${slug}`);
  await expect(page).toHaveURL(new RegExp(`#/title/${slug}`));
  await expect(page.locator('.detail-page').getByRole('heading', { name: title })).toBeVisible();
}

async function apiLogin(request) {
  const csrfResponse = await request.get('/api/csrf');
  expect(csrfResponse.status()).toBe(200);
  const csrf = await csrfResponse.json();

  const loginResponse = await request.post('/api/auth/login', {
    headers: {
      [csrf.headerName]: csrf.token,
    },
    data: credentials,
  });
  expect(loginResponse.status()).toBe(200);
  return csrf;
}

test.describe('Streamfolio e2e', () => {
  test('parcours principal : login, recherche, fiche, watchlist, lecteur, logout', async ({ page }) => {
    await login(page);

    await page.getByRole('searchbox', { name: 'Rechercher' }).fill('botanical');
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/#\/catalog\?query=botanical/);
    await expect(page.locator('#main-content')).toContainText('Botanical Cities');

    await openTitleDetail(page, 'botanical-cities', 'Botanical Cities');

    await page.locator('.detail-page').getByRole('button', { name: /Ma liste/ }).click();
    await page.getByRole('link', { name: 'Ma liste' }).first().click();
    await expect(page.locator('#main-content')).toContainText('Botanical Cities');

    await page.getByRole('button', { name: /Lire Botanical Cities|Lire/ }).first().click();
    await expect(page.locator('#video-player')).toBeVisible();
    await expect(page.locator('#player-mode')).toContainText(/MP4|HLS/);

    await page.getByLabel(/Se déconnecter/).click();
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
  });

  test('inscription : le nouveau compte USER ne voit pas et ne peut pas utiliser l’administration', async ({ page }) => {
    const username = `viewer_${Date.now()}`;

    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
    await page.getByRole('link', { name: 'Créer un compte utilisateur' }).click();
    await expect(page.getByRole('heading', { name: 'Inscription' })).toBeVisible();

    await page.getByLabel("Nom d'utilisateur").fill(username);
    await page.getByLabel('Mot de passe').fill('password123');
    await page.getByRole('button', { name: 'Créer le compte' }).click();

    await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Admin' })).toHaveCount(0);

    const me = await page.request.get('/api/me');
    expect(me.status()).toBe(200);
    const mePayload = await me.json();
    expect(mePayload.displayName).toBe(username);
    expect(mePayload.roles).toEqual(['USER']);

    const adminApi = await page.request.get('/api/admin/videos');
    expect(adminApi.status()).toBe(403);

    await page.goto('/#/admin');
    await expect(page).toHaveURL(/#\/$/);
    await expect(page.getByRole('link', { name: 'Admin' })).toHaveCount(0);
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
