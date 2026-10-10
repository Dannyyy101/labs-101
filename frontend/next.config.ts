import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  experimental: {
    serverActions: {
      // photos of meals, they are downscaled in the browser but the original is sent if that fails
      bodySizeLimit: "11mb",
    },
  },
  images: {
    remotePatterns: [new URL('https://lh3.googleusercontent.com/**'), new URL('https://auth.project101.tech/**')],
  },
};

export default nextConfig;
