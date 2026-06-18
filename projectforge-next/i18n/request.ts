import { getRequestConfig } from "next-intl/server";

// TODO: source the locale from a cookie / Accept-Language once the user is in
// scope. For now we ship German UI to match the design.
export default getRequestConfig(async () => {
  const locale = "de";
  return {
    locale,
    messages: (await import(`../messages/${locale}.json`)).default,
  };
});
