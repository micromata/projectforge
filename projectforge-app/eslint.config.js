// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ['dist/*', 'ios/*', 'android/*'],
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    ignores: ['src/**/__tests__/**', 'src/testing/**'],
    rules: {
      // React Native's global URL isn't WHATWG conformant (src/core/url.ts): parseUrl/SafeURL only.
      'no-restricted-globals': [
        'error',
        { name: 'URL', message: 'Use parseUrl/SafeURL of src/core/url.ts.' },
        { name: 'URLSearchParams', message: 'Use SafeURLSearchParams of src/core/url.ts.' },
        { name: 'fetch', message: 'Use platformHttp (expo/fetch) with HTTP_DEFAULTS, see src/core/api/http.ts.' },
      ],
    },
  },
]);
