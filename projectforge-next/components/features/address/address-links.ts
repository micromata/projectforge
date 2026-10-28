/**
 * A website as typed ("www.example.org") is no absolute url, so the link gets a scheme. Only http(s) is linked as
 * it is: the value is free text, and a `javascript:` url would run in this page.
 */
export function websiteHref(website: string): string {
  const trimmed = website.trim();
  return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
}
