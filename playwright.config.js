const { defineConfig, devices } = require('@playwright/test');

const baseURL = process.env.BASE_URL || 'http://127.0.0.1:8080';
const skipWebServer = process.env.PLAYWRIGHT_SKIP_WEBSERVER === 'true';
const reuseWebServer = process.env.PLAYWRIGHT_REUSE_SERVER === 'true';
const redisURL = process.env.STREAMFOLIO_REDIS_URL || 'redis://localhost:6379';
const webServerCommand = process.env.STREAMFOLIO_REDIS_URL
  ? 'mvn -q -f backend/pom.xml spring-boot:run'
  : 'docker compose up -d redis && mvn -q -f backend/pom.xml spring-boot:run';

module.exports = defineConfig({
  testDir: './tests/e2e',
  timeout: 45000,
  expect: {
    timeout: 10000,
  },
  fullyParallel: false,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [['list'], ['html', { outputFolder: 'build/playwright-report', open: 'never' }]]
    : [['list']],
  outputDir: 'build/playwright-results',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: skipWebServer ? undefined : {
    command: webServerCommand,
    env: { ...process.env, STREAMFOLIO_REDIS_URL: redisURL },
    url: `${baseURL}/api/health`,
    reuseExistingServer: reuseWebServer,
    timeout: 120000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
