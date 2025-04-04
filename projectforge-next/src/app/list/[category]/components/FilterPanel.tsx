'use client'

import { useState, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type FilterOption = {
  id: string
  displayName: string
}

type Filter = {
  id: string
  key: string
  label: string
  filterType: string
  values?: FilterOption[]
  multi?: boolean
  defaultFilter?: boolean
  openInterval?: boolean
  selectors?: string[]
}

interface FilterPanelProps {
  filters: Filter[]
  onApplyFilters: (filters: Record<string, any>) => void
}

export default function FilterPanel({ filters, onApplyFilters }: FilterPanelProps) {
  const [activeFilters, setActiveFilters] = useState<Record<string, any>>({})
  const [expanded, setExpanded] = useState(false)

  // Handle filter change
  const handleFilterChange = useCallback((filterId: string, value: any) => {
    setActiveFilters((prev) => ({
      ...prev,
      [filterId]: value,
    }))
  }, [])

  // Handle form submission
  const handleSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault()
    onApplyFilters(activeFilters)
  }, [activeFilters, onApplyFilters])

  // Handle reset
  const handleReset = useCallback(() => {
    setActiveFilters({})
  }, [])

  // Toggle expanded state
  const toggleExpanded = useCallback(() => {
    setExpanded((prev) => !prev)
  }, [])

  // Render input based on filter type
  const renderFilterInput = useCallback((filter: Filter) => {
    switch (filter.filterType) {
      case 'STRING':
        return (
          <Input
            id={filter.id}
            value={activeFilters[filter.id] || ''}
            onChange={(e) => handleFilterChange(filter.id, e.target.value)}
            placeholder={filter.label}
          />
        )

      case 'BOOLEAN':
        return (
          <div className="flex items-center space-x-2">
            <Checkbox
              id={filter.id}
              checked={activeFilters[filter.id] || false}
              onCheckedChange={(checked) => 
                handleFilterChange(filter.id, checked === true ? true : false)
              }
            />
            <label 
              htmlFor={filter.id} 
              className="text-sm leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
            >
              {filter.label}
            </label>
          </div>
        )

      case 'LIST':
        return (
          <Select
            value={activeFilters[filter.id] || ''}
            onValueChange={(value) => handleFilterChange(filter.id, value)}
          >
            <SelectTrigger>
              <SelectValue placeholder={`Select ${filter.label}`} />
            </SelectTrigger>
            <SelectContent>
              {filter.values?.map((option) => (
                <SelectItem key={option.id} value={option.id}>
                  {option.displayName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )

      case 'TIMESTAMP':
        return (
          <div className="flex flex-col space-y-2">
            <div className="space-y-1">
              <label htmlFor={`${filter.id}-from`} className="text-xs">From</label>
              <Input
                type="date"
                id={`${filter.id}-from`}
                value={activeFilters[`${filter.id}-from`] || ''}
                onChange={(e) => handleFilterChange(`${filter.id}-from`, e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <label htmlFor={`${filter.id}-to`} className="text-xs">To</label>
              <Input
                type="date"
                id={`${filter.id}-to`}
                value={activeFilters[`${filter.id}-to`] || ''}
                onChange={(e) => handleFilterChange(`${filter.id}-to`, e.target.value)}
              />
            </div>
          </div>
        )

      default:
        return (
          <Input
            id={filter.id}
            value={activeFilters[filter.id] || ''}
            onChange={(e) => handleFilterChange(filter.id, e.target.value)}
            placeholder={filter.label}
          />
        )
    }
  }, [activeFilters, handleFilterChange])

  // For mobile, we'll show fewer filters and add a "Show more" button
  const displayedFilters = expanded ? filters : filters.slice(0, 5)

  return (
    <form onSubmit={handleSubmit} className="space-y-6 bg-gray-50 p-4 rounded-lg">
      <div className="font-medium text-lg mb-4">Filter</div>
      
      <div className="space-y-4">
        {displayedFilters.map((filter) => (
          <div key={filter.id} className="space-y-2">
            {filter.filterType !== 'BOOLEAN' && (
              <label htmlFor={filter.id} className="text-sm font-medium">
                {filter.label}
              </label>
            )}
            {renderFilterInput(filter)}
          </div>
        ))}
        
        {filters.length > 5 && (
          <button
            type="button"
            className="text-sm text-blue-600 hover:text-blue-800"
            onClick={toggleExpanded}
          >
            {expanded ? 'Show fewer filters' : 'Show more filters'}
          </button>
        )}
      </div>
      
      <div className="flex gap-2">
        <Button type="submit" className="w-full">
          Apply
        </Button>
        <Button 
          type="button" 
          onClick={handleReset}
          variant="outline"
          className="w-full"
        >
          Reset
        </Button>
      </div>
    </form>
  )
}