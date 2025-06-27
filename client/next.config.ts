import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./src/config/i18n.ts");

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  compiler: {
    styledComponents: true,
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com",
        pathname: "/a/**",
      },
      {
        protocol: "https",
        hostname: "img.vietqr.io",
        pathname: "/image/**",
      },
      {
        protocol: "https",
        hostname: "api.vietqr.io",
        pathname: "/img/**",
      },
      {
        protocol: "https",
        hostname: "vietqr.net",
        pathname: "/portal-service/resources/icons/**",
      },
      {
        protocol: "http",
        hostname: "localhost",
        pathname: "/rails/active_storage/**",
      },
      {
        protocol: "https",
        hostname:
          process.env.NEXT_PUBLIC_BACKEND_HOSTNAME || "backend.snow.exchange",
        pathname: "/rails/active_storage/**",
      },
      {
        protocol: "https",
        hostname: "backend.snow.exchange",
        pathname: "/rails/active_storage/**",
      },
    ],
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          {
            key: "Cross-Origin-Opener-Policy",
            value: "same-origin-allow-popups",
          },
        ],
      },
    ];
  },
};

export default withNextIntl(nextConfig);
