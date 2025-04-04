'use client'

import { useParams } from 'next/navigation'
import { useState, useEffect, useCallback } from 'react'
import ListTable from './components/ListTable'
import FilterPanel from './components/FilterPanel'
import { Button } from '@/components/ui/button'

export default function ListPage() {
  const { category } = useParams()
  const [data, setData] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showFilterPanel, setShowFilterPanel] = useState(true)
  
  // Load data callback
  const loadData = useCallback(async () => {
    try {
      setIsLoading(true)
      // In production, this would be a fetch call to your API
      // const response = await fetch(`/api/list/${category}`)
      // const result = await response.json()
      const result = await import('./exampleListRequest.json')
      setData(result.default)
      setError(null)
    } catch (err) {
      setError(err)
      setData(null)
    } finally {
      setIsLoading(false)
    }
  }, [category])
  
  // Handle applying filters
  const handleFilterApply = useCallback((filters) => {
    console.log('Applying filters:', filters)
    // In a real app, you would fetch filtered data here
    // For now, we'll just log the filters
  }, [])
  
  // Toggle filter panel visibility
  const toggleFilterPanel = useCallback(() => {
    setShowFilterPanel(prev => !prev)
  }, [])
  
  useEffect(() => {
    loadData()
  }, [loadData])

  if (isLoading) return <div className="flex justify-center items-center min-h-screen">Loading...</div>
  if (error) return <div className="flex justify-center items-center min-h-screen">Error loading data</div>
  if (!data) return <div className="flex justify-center items-center min-h-screen">No data available</div>

  return (
    <div className="container mx-auto p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">{data.ui.title}</h1>
        <Button
          onClick={toggleFilterPanel}
          variant="outline"
          size="sm"
        >
          {showFilterPanel ? 'Hide Filters' : 'Show Filters'}
        </Button>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {showFilterPanel && (
          <div className="lg:col-span-1">
            <FilterPanel 
              filters={data.ui.namedContainers.find(c => c.id === 'searchFilter')?.content || []} 
              onApplyFilters={handleFilterApply}
            />
          </div>
        )}
        
        <div className={showFilterPanel ? "lg:col-span-3" : "lg:col-span-4"}>
          <ListTable 
            columns={data.ui.layout.find(l => l.id === 'resultSet')?.columnDefs || []} 
            data={data.data?.resultSet || []}
          />
        </div>
      </div>
    </div>
  )
}