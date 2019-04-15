# Setup WebApp locally
1. Ensure that `yarn` or `npm` is installed.
2. Install the dependencies with `yarn install` or `npm install`.

## Testing
You can test the webapp via `yarn test` or `npm run tests`. It will run all tests and check the test coverage.

## Building
You can build the webapp via `yarn build` or `npm run build`.

## Development with hot-code-replacement (recommended)
1. Enable cross origin in file `$HOME/ProjectForge/projectforge.properties`: `projectforge.web.development.enableCORSFilter=true`
2. Start ProjectForge. See [ProjectForge ReadMe](../README.adoc)
3. Start web server with hot-code-replacement:
   1. Run `yarn start` or `npm start` in terminal.
   2. Open browser (if not started automatically) on yarn/npm port (default is 3000).
   
Now you can modify the Web files directly in the directory ```projectforge-webapp``` and
any change is automatically deployed instantly for your web browser.

