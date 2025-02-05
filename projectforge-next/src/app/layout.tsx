import type { Metadata } from 'next'
import { GeistSans } from 'geist/font/sans'
import './globals.css'
import Image from 'next/image'
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from '@/components/ui/navigation-menu'
import { SquareMenu } from 'lucide-react'

export const metadata: Metadata = {
  title: 'ProjetForge',
  description:
    'Improve your team work and keep track of budgets, deadlines, human resources and your financial management.',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="de">
      <body className={`${GeistSans.className} antialiased`}>
        <div className="min-h-screen flex flex-col">
          {/* Header */}
          <header className="border-b bg-white">
            <div className="mx-auto px-4 h-16 flex items-center justify-between">
              <div className="flex items-center gap-8">
                {/* Primary Logo */}
                <Image src="/logo.png" alt="Company Logo" width={227} height={50} />
                {/* Secondary Logo */}
                <Image src="/projectforge.png" alt="Company Logo" width={65} height={43} />
              </div>

              <button className="flex items-center gap-2 text-sm text-gray-700 hover:text-gray-900">
                <span>Fin Reinhard</span>
                {/*<ChevronDown size={16} />*/}
              </button>
            </div>
          </header>

          {/* Navigation */}
          <nav className="border-b bg-gray-50">
            <div className="mx-auto px-4">
              <NavigationMenu>
                <NavigationMenuList>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    <SquareMenu strokeWidth={1} />
                  </NavigationMenuItem>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    Projektmanagement
                  </NavigationMenuItem>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    Strukturbaum
                  </NavigationMenuItem>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    Kalender
                  </NavigationMenuItem>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    Adressen
                  </NavigationMenuItem>
                  <NavigationMenuItem className="px-4 py-2 hover:bg-gray-100">
                    Datentransfer
                  </NavigationMenuItem>
                </NavigationMenuList>
              </NavigationMenu>
            </div>
          </nav>

          {/* Main Content */}
          <main className="flex-1 px-4 py-8">{children}</main>

          {/* Footer */}
          <footer className="border-t bg-gray-50">
            <div className="mx-auto px-4 h-16 flex items-center justify-between text-sm text-gray-600">
              <div>©2001-2025 Micromata GmbH</div>
              <div className="flex gap-4">
                <a href="http://www.projectforge.org" className="hover:text-gray-900">
                  www.projectforge.org
                </a>
                <a
                  href="https://github.com/micromata/projectforge/"
                  className="hover:text-gray-900"
                >
                  GitHub Repository
                </a>
              </div>
            </div>
          </footer>
        </div>
      </body>
    </html>
  )
}
