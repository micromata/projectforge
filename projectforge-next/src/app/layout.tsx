import type { Metadata } from 'next'
import { GeistSans } from 'geist/font/sans'
import './globals.css'
import Image from 'next/image'
import { Calendar, MenuSquare, TreePalm, User } from 'lucide-react'
import { GlobalNavigation } from '@/components/globalNavigation'
import { MobileNavigationTab } from '@/components/globalNavigation/MobileNavigationTab'
import {
  mainContentId,
  GlobalNavigationTrigger,
} from '@/components/globalNavigation/GlobalNavigationTrigger'

export const metadata: Metadata = {
  title: 'ProjectForge',
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
        <div className="h-screen flex flex-col md:flex-row bg-zinc-50 group/gn relative">
          <GlobalNavigation />
          {/* Move Main window to the right when gn-trigger is checked */}
          <GlobalNavigationTrigger />
          <div
            id={mainContentId}
            className="bg-white md:bg-transparent flex-1 transform translate-x-0 scale-100 transition-all border-b border-zinc-300 rounded-none duration-300 ease-in-out peer-checked:translate-x-[90%] peer-checked:rounded peer-checked:scale-[99%] peer-checked:border-transparent"
          >
            <Image
              src="/logo.png"
              alt="Company Logo"
              width={227}
              height={50}
              className="md:hidden"
            />
            <div className="bg-white md:m-4 md:rounded-lg md:border md:border-zinc-200 md:inset-shadow-sm md:inset-shadow-zinc-900 md:overflow-auto px-2 md:p-4">
              {children}
            </div>
          </div>
          <div className="flex justify-between items-center pb-8 pt-4 px-8 md:hidden">
            <label
              htmlFor="gn-trigger"
              className="group-has-[#gn-trigger:checked]/gn:text-blue-700"
            >
              <MobileNavigationTab icon={<MenuSquare />}>Menu</MobileNavigationTab>
            </label>
            <MobileNavigationTab icon={<Calendar />}>Kalender</MobileNavigationTab>
            <MobileNavigationTab icon={<TreePalm />}>Urlaub</MobileNavigationTab>
            <MobileNavigationTab icon={<User />}>Fin R.</MobileNavigationTab>
          </div>
        </div>
      </body>
    </html>
  )
}
