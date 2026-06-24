import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      { source: "/rs/:path*", destination: "http://localhost:8080/rs/:path*" },
      {
        source: "/rsPublic/:path*",
        destination: "http://localhost:8080/rsPublic/:path*",
      },
    ];
  },
};

export default withNextIntl(nextConfig);
