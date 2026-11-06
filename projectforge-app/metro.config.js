// The Metro config: the shared TypeScript of ../projectforge-shared (a symlink in node_modules/@projectforge/shared,
// "file:" dependency) is outside the project root, so Metro watches it and resolves its imports from here.
const path = require('node:path');
const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);
const shared = path.resolve(__dirname, '../projectforge-shared');

config.watchFolders = [...(config.watchFolders ?? []), shared];
config.resolver.nodeModulesPaths = [path.resolve(__dirname, 'node_modules')];

module.exports = config;
