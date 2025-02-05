'use client'

import { ChangeEvent, useCallback, useEffect, useRef } from 'react'

// ID used to target main content area for click handling
export const mainContentId = 'main-content'

export const GlobalNavigationTrigger = () => {
  // Ref to directly access checkbox DOM element
  const ref = useRef<HTMLInputElement>(null)

  // Handler to close navigation when main content is clicked
  const handleMainContentClick = useCallback(() => {
    if (ref.current && ref.current.checked) {
      ref.current.checked = false

      document.getElementById(mainContentId)?.removeEventListener('click', handleMainContentClick)
      return
    }
  }, [])

  // Manages event listeners based on checkbox state
  const handleChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const { checked } = event.target
      console.log({ checked })

      // Add click listener to main content when nav opens
      if (checked) {
        document.getElementById(mainContentId)?.addEventListener('click', handleMainContentClick)
      } else {
        // Remove listener when nav closes
        document.getElementById(mainContentId)?.removeEventListener('click', handleMainContentClick)
      }
    },
    [handleMainContentClick]
  )

  // Cleanup event listeners on component unmount
  useEffect(() => {
    return () => {
      document.getElementById(mainContentId)?.removeEventListener('click', handleMainContentClick)
    }
  }, [handleMainContentClick])

  // Hidden checkbox controlled by external label
  return (
    <input
      ref={ref}
      type="checkbox"
      id="gn-trigger"
      className="hidden peer"
      onChange={handleChange}
    />
  )
}
