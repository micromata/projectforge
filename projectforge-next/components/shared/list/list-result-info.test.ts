import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ListResultInfo } from "./list-result-info";

const render = (info: string) =>
  renderToStaticMarkup(createElement(ListResultInfo, { info }));

describe("ListResultInfo", () => {
  it("keeps the backend's truncation span with its style", () => {
    const html = render(
      '<span style="color:red; font-weight: bold;">Too many results.</span>'
    );
    expect(html).toContain('<span style="color:red;font-weight:bold">');
    expect(html).toContain("Too many results.");
  });

  it("drops a span style with other css", () => {
    const html = render(
      '<span style="position:fixed; inset:0; background:white">Fake</span>'
    );
    expect(html).not.toContain("style=");
    expect(html).toContain("Fake");
  });

  it("removes script and event handlers from raw html", () => {
    const html = render(
      'XSS_PROOF_<img src=x onerror="alert(1)"><script>alert(2)</script>'
    );
    expect(html).not.toContain("onerror");
    expect(html).not.toContain("<script");
  });
});
