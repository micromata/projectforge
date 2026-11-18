# @projectforge/plugin-sdk

Types of the contract between the app and a frontend plugin. The implementation is the bridge of the app
(`lib/plugin-runtime/bridge.ts`, `HostPluginRuntime`); the pieces it hands out (design system, formatting, data access, ...)
are listed in `dev-docs/next/plugin-frontend.md`. `PLUGIN_SDK_VERSION` changes only with a breaking change of the
contract; a plugin built for another version is refused by the app.
