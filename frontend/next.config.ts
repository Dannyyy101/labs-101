import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [new URL('https://lh3.googleusercontent.com/**'), new URL('https://auth.project101.tech/**')],
  },
};

export default nextConfig;
