import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  /* config options here */
  
  // Add rewrites to proxy API requests to the backend server
  async rewrites() {
    return [
      {
        source: '/rs/:path*',
        destination: 'http://localhost:8080/rs/:path*',
      },
    ]
  },
}

export default nextConfig
