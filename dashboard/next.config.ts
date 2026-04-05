import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "books.google.com",
      },
      {
        protocol: "https",
        hostname: "covers.openlibrary.org",
      },
      {
        protocol: "https",
        hostname: "*.casadellibro.com",
      },
      {
        protocol: "https",
        hostname: "casadellibro.com",
      },
    ],
  },
};

export default nextConfig;
