import { describe, expect, it } from "vitest";
import { resolveMenuUrl, sanitizeRedirectUrl, toAbsoluteUrl } from "./menu-url";

/**
 * The redirect targets of `/next/login` (`?returnUrl=`, the server's `redirectUrl`) and of every
 * UILayout action end up in `window.location`/`router.push`, so an attacker-supplied one would be an
 * open redirect — and a convincing phishing hop, because the victim really did log in to
 * ProjectForge. The backend applies the same rule in `LoginServiceRest.sanitizeRedirectUrl`
 * (`LoginServiceRestTest` covers it there); this is the client's half, which protects the
 * client-side navigations the server never sees.
 */
describe("sanitizeRedirectUrl", () => {
  it("drops urls naming a foreign host", () => {
    // Browsers read all of these as a host, not as a path of this application.
    for (const url of [
      "http://evil.example",
      "https://evil.example/phish",
      "//evil.example",
      "///evil.example",
      "\\\\evil.example",
      "/\\evil.example",
      "\\/evil.example",
    ]) {
      expect(sanitizeRedirectUrl(url), url).toBeNull();
    }
  });

  it("drops other schemes", () => {
    for (const url of [
      "javascript:alert(1)",
      "data:text/html,<script>alert(1)</script>",
      "mailto:foo@example.com",
      "vbscript:msgbox(1)",
    ]) {
      expect(sanitizeRedirectUrl(url), url).toBeNull();
    }
  });

  it("drops schemes hidden behind control characters", () => {
    // A browser strips tab/newline/NUL from inside a url before parsing it, so each of these is
    // `javascript:` by the time it navigates — while a plain `startsWith` check sees something else.
    for (const url of [
      "ja\tvascript:alert(1)",
      "java\nscript:alert(1)",
      "javasc\r\nript:alert(1)",
      "\u0000javascript:alert(1)",
      "java\u0000script:alert(1)",
    ]) {
      expect(sanitizeRedirectUrl(url), JSON.stringify(url)).toBeNull();
    }
    // Same for a host smuggled past the "//" check by a leading control character.
    expect(sanitizeRedirectUrl("\t//evil.example")).toBeNull();
    expect(sanitizeRedirectUrl("\u0000//evil.example")).toBeNull();
  });

  it("drops paths that walk out of the application", () => {
    // `/next/../..//evil.example` is `//evil.example` once the browser normalizes it — every segment
    // looked like a path, the result is a foreign host.
    for (const url of [
      "/next/../..//evil.example",
      "/next/..//evil.example",
      "/next/react/../..//evil.example",
      "/..",
      "/next/..",
      "/next/..\\..//evil.example",
    ]) {
      expect(sanitizeRedirectUrl(url), url).toBeNull();
    }
  });

  it("keeps a `..` that is only part of a query value", () => {
    // Data, not a path segment: the browser never resolves it.
    expect(sanitizeRedirectUrl("/next/book/?filter=../x")).toBe(
      "/next/book/?filter=../x"
    );
  });

  it("has nothing to redirect to for empty input", () => {
    for (const url of [null, undefined, "", "   ", "\t\n"]) {
      expect(sanitizeRedirectUrl(url), JSON.stringify(url)).toBeNull();
    }
  });

  it("keeps the legitimate targets unchanged", () => {
    for (const url of [
      "/next/book/42",
      "/next/",
      "/react/calendar",
      "/wa/taskTree",
      "/next/book?filter=abc&sort=title",
      "/next/book/#anchor",
      // Relative, as the UILayout protocol sends them (PagesResolver.getDynamicPageUrl with
      // `absolute = false`). They resolve against the current page, so they stay on this origin.
      "vacationAccount/recalculate",
      "logout",
    ]) {
      expect(sanitizeRedirectUrl(url), url).toBe(url);
    }
  });
});

describe("resolveMenuUrl", () => {
  it("routes this app's own urls internally, without the base path", () => {
    // next/link prepends the base path again via `basePath`, so it must not be doubled here.
    expect(resolveMenuUrl("/next/book/42")).toEqual({
      kind: "internal",
      href: "/book/42",
    });
    expect(resolveMenuUrl("next/book/42")).toEqual({
      kind: "internal",
      href: "/book/42",
    });
    expect(resolveMenuUrl("/next")).toEqual({ kind: "internal", href: "/" });
    // Unprefixed urls are this app's routes too.
    expect(resolveMenuUrl("logout")).toEqual({
      kind: "internal",
      href: "/logout",
    });
  });

  it("leaves the other frontends to a full page load", () => {
    expect(resolveMenuUrl("/react/calendar")).toEqual({
      kind: "external",
      href: "/react/calendar",
    });
    expect(resolveMenuUrl("wa/taskTree")).toEqual({
      kind: "external",
      href: "/wa/taskTree",
    });
    expect(resolveMenuUrl("https://www.projectforge.org")).toEqual({
      kind: "external",
      href: "https://www.projectforge.org",
    });
  });

  it("does not turn a `/next//host` url into a host", () => {
    // Stripping the `next/` prefix off `/next//evil.example` would leave `//evil.example`. Callers
    // pass it through `sanitizeRedirectUrl` first (which rejects it), but the base path is the only
    // thing keeping the bare result on this origin — so don't hand a host out of here either.
    expect(resolveMenuUrl("/next//evil.example")).toEqual({
      kind: "internal",
      href: "/",
    });
    expect(resolveMenuUrl("/next/\\evil.example")).toEqual({
      kind: "internal",
      href: "/",
    });
  });

  it("has no target for an empty url", () => {
    expect(resolveMenuUrl(null)).toEqual({ kind: "internal", href: "#" });
    expect(resolveMenuUrl("")).toEqual({ kind: "internal", href: "#" });
  });
});

describe("toAbsoluteUrl", () => {
  it("prepends the base path for this app, and nothing else", () => {
    expect(toAbsoluteUrl({ kind: "internal", href: "/book/42" })).toBe(
      "/next/book/42"
    );
    expect(toAbsoluteUrl({ kind: "external", href: "/wa/taskTree" })).toBe(
      "/wa/taskTree"
    );
    // The placeholder of a target that is still loading stays a placeholder.
    expect(toAbsoluteUrl({ kind: "internal", href: "#" })).toBe("#");
  });
});
