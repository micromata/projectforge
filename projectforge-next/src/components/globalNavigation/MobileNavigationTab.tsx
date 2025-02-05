import { cn } from '@/lib/utils'
import { PropsWithChildren } from 'react'

interface MobileNavigationTabProps {
  icon: React.ReactNode
  className?: string
}

export const MobileNavigationTab = (props: PropsWithChildren<MobileNavigationTabProps>) => (
  <div
    className={cn(
      'flex flex-col items-center justify-center gap-2 cursor-pointer',
      props.className
    )}
  >
    {props.icon}
    <span className="text-xs">{props.children}</span>
  </div>
)
