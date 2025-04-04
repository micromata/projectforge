'use client'

import { useMemo, useState, useCallback, useRef, useEffect } from 'react'
import Link from 'next/link'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable,
  Row,
  ColumnOrderState,
  VisibilityState,
  ColumnResizeMode,
  ColumnPinningState,
} from '@tanstack/react-table'
import { ArrowUpDown, Edit, Pin, Eye, EyeOff, GripVertical } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { cn } from '@/lib/utils'

type Column = {
  field: string
  headerName: string
  sortable?: boolean
  resizable?: boolean
  width?: number
  filter?: string
  filterParams?: any
  valueGetter?: string
  headerTooltip?: string
  headerClass?: string[]
  cellRenderer?: string
  cellRendererParams?: any
  dataType?: string
  formatter?: string
  valueIconMap?: Record<string, string[]>
  type?: string
  key?: string
}

type ListItem = {
  id: number
  deleted?: boolean
  [key: string]: any
}

interface ListTableProps {
  columns: Column[]
  data: ListItem[]
}

export default function ListTable({ columns, data }: ListTableProps) {
  const [sorting, setSorting] = useState<SortingState>([])
  // Column customization states
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({})
  const [columnOrder, setColumnOrder] = useState<ColumnOrderState>([])
  const [columnPinning, setColumnPinning] = useState<ColumnPinningState>({})
  const [columnResizeMode, setColumnResizeMode] = useState<ColumnResizeMode>('onChange')
  
  // Refs for column drag and drop
  const draggedColumnRef = useRef<string | null>(null)
  const dragOverColumnRef = useRef<string | null>(null)
  
  // Initialize column order on mount
  useEffect(() => {
    if (columns.length > 0 && columnOrder.length === 0) {
      setColumnOrder(columns.map(col => col.field).concat(['actions']))
    }
  }, [columns, columnOrder.length])

  // Helper function to get cell value based on column id
  const getCellValue = useCallback((item: ListItem, columnId: string) => {
    // Handle two possibilities:
    // 1. If columnId contains a dot (like 'address.name'), extract object and property
    // 2. If columnId is a direct field (like 'title'), access it directly
    if (columnId.includes('.')) {
      const [object, property] = columnId.split('.')
      if (!item[object] || !item[object][property]) {
        return ''
      }
      return item[object][property]
    } else {
      // Direct field access
      return item[columnId] !== undefined ? item[columnId] : ''
    }
  }, [])

  // Format cell based on data type and formatter
  const formatCell = useCallback((value: any, column: Column) => {
    if (value === undefined || value === null) {
      return ''
    }

    // Handle icons from valueIconMap
    if (column?.valueIconMap && column.valueIconMap[String(value)]) {
      const [icon, name] = column.valueIconMap[String(value)]
      return <span className="text-gray-600"><i className={`${icon} fa-${name}`}></i></span>
    }

    // Handle different data types
    switch (column?.dataType) {
      case 'TIMESTAMP':
        return new Date(value).toLocaleDateString()
        
      case 'CUSTOMIZED':
        if (column.id === 'address.phoneNumbers') {
          // This would be customized based on your needs
          return 'Custom Phone Format'
        } else if (column.id === 'address.imagePreview') {
          return value ? <div className="w-8 h-8 rounded-full bg-gray-200"></div> : null
        }
        return value
        
      default:
        return value
    }
  }, [])

  // Column drag and drop handlers
  const handleDragStart = useCallback((columnId: string) => {
    draggedColumnRef.current = columnId
  }, [])

  const handleDragOver = useCallback((event: React.DragEvent<HTMLElement>, columnId: string) => {
    event.preventDefault()
    dragOverColumnRef.current = columnId
  }, [])

  const handleDrop = useCallback((event: React.DragEvent<HTMLElement>) => {
    event.preventDefault()
    
    if (draggedColumnRef.current && dragOverColumnRef.current) {
      const newColumnOrder = [...columnOrder]
      const draggedIndex = newColumnOrder.indexOf(draggedColumnRef.current)
      const dropIndex = newColumnOrder.indexOf(dragOverColumnRef.current)
      
      if (draggedIndex >= 0 && dropIndex >= 0) {
        newColumnOrder.splice(draggedIndex, 1)
        newColumnOrder.splice(dropIndex, 0, draggedColumnRef.current)
        setColumnOrder(newColumnOrder)
      }
      
      // Reset refs
      draggedColumnRef.current = null
      dragOverColumnRef.current = null
    }
  }, [columnOrder])

  // Column visibility handler
  const toggleColumnVisibility = useCallback((columnId: string) => {
    setColumnVisibility(prev => ({
      ...prev,
      [columnId]: !prev[columnId]
    }))
  }, [])

  // Column pinning handler
  const toggleColumnPin = useCallback((columnId: string) => {
    setColumnPinning(prev => {
      // If already pinned, unpin it
      if (prev.left?.includes(columnId)) {
        return {
          ...prev,
          left: prev.left.filter(id => id !== columnId)
        }
      }
      
      // Otherwise pin it to the left
      return {
        ...prev,
        left: [...(prev.left || []), columnId]
      }
    })
  }, [])

  // Define table columns
  const tableColumns = useMemo<ColumnDef<ListItem>[]>(() => {
    const cols = columns.map((column) => {
      return {
        accessorFn: (row) => getCellValue(row, column.field),
        id: column.field,
        header: ({ column: tableColumn }) => {
          return (
            <div className="flex items-center group">
              {/* Drag handle */}
              <div 
                className="cursor-grab mr-2 opacity-0 group-hover:opacity-100" 
                draggable={true}
                onDragStart={() => handleDragStart(column.field)}
                onDragOver={(e) => handleDragOver(e, column.field)}
                onDrop={handleDrop}
              >
                <GripVertical className="h-4 w-4" />
              </div>
              
              {/* Column title with sort button if sortable */}
              <div className="flex-1">
                {column.sortable ? (
                  <button
                    className="flex items-center gap-2"
                    onClick={() => tableColumn.toggleSorting(tableColumn.getIsSorted() === "asc")}
                  >
                    {column.headerName}
                    <ArrowUpDown className="ml-2 h-4 w-4" />
                  </button>
                ) : (
                  <span>{column.headerName}</span>
                )}
              </div>
              
              {/* Column controls */}
              <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100">
                {/* Pin toggle */}
                <button 
                  className={cn(
                    "p-0.5 rounded hover:bg-gray-200",
                    columnPinning.left?.includes(column.field) ? "text-blue-500" : "text-gray-400"
                  )}
                  onClick={() => toggleColumnPin(column.field)}
                  title={columnPinning.left?.includes(column.field) ? "Unpin column" : "Pin column"}
                >
                  <Pin className="h-3 w-3" />
                </button>
                
                {/* Visibility toggle */}
                <button 
                  className="p-0.5 rounded hover:bg-gray-200 text-gray-400"
                  onClick={() => toggleColumnVisibility(column.field)}
                  title="Hide column"
                >
                  <EyeOff className="h-3 w-3" />
                </button>
              </div>
            </div>
          )
        },
        cell: ({ row }) => {
          const value = getCellValue(row.original, column.field)
          return formatCell(value, column)
        },
        enableSorting: column.sortable || false,
        enableResizing: column.resizable || true,
        size: column.width,
      }
    })
    
    // Add action column
    cols.push({
      id: 'actions',
      header: "Actions",
      cell: ({ row }) => {
        return (
          <div className="flex justify-end">
            <Link 
              href={`/edit/${row.original.id}`}
              className="flex items-center text-blue-600 hover:text-blue-800"
            >
              <Edit className="h-4 w-4 mr-1" />
              <span>Edit</span>
            </Link>
          </div>
        )
      },
      enableResizing: true,
    })
    
    return cols
  }, [
    columns, 
    getCellValue, 
    formatCell, 
    handleDragStart, 
    handleDragOver, 
    handleDrop, 
    toggleColumnPin, 
    toggleColumnVisibility, 
    columnPinning.left
  ])

  // Table instance
  const table = useReactTable({
    data,
    columns: tableColumns,
    state: {
      sorting,
      columnVisibility,
      columnOrder,
      columnPinning,
    },
    onSortingChange: setSorting,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnOrderChange: setColumnOrder,
    onColumnPinningChange: setColumnPinning,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    columnResizeMode,
  })

  // Column management panel
  const ColumnManager = () => {
    return (
      <div className="bg-white p-4 border rounded-md mb-4">
        <h3 className="font-medium text-sm mb-2">Column Management</h3>
        <div className="flex flex-wrap gap-2">
          {columns.map(column => (
            <div key={column.field} className="flex items-center space-x-2 border rounded p-2">
              <Checkbox 
                id={`visibility-${column.field}`}
                checked={!columnVisibility[column.field]}
                onCheckedChange={() => toggleColumnVisibility(column.field)}
              />
              <label htmlFor={`visibility-${column.field}`} className="text-sm">
                {column.headerName}
              </label>
              <button 
                className={cn(
                  "p-1 rounded hover:bg-gray-100", 
                  columnPinning.left?.includes(column.field) ? "text-blue-500" : "text-gray-400"
                )}
                onClick={() => toggleColumnPin(column.field)}
                title={columnPinning.left?.includes(column.field) ? "Unpin column" : "Pin column"}
              >
                <Pin className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <ColumnManager />
      
      <div className="rounded-md border overflow-x-auto">
        <div className="relative">
          <Table style={{ width: table.getCenterTotalSize() }}>
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead 
                      key={header.id}
                      style={{
                        width: header.getSize(),
                        position: header.column.getIsPinned() ? 'sticky' : 'relative',
                        left: header.column.getIsPinned() === 'left' ? `${header.getStart()}px` : undefined,
                        background: header.column.getIsPinned() ? 'white' : undefined,
                        zIndex: header.column.getIsPinned() ? 1 : undefined,
                      }}
                      className="relative"
                    >
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                      <div
                        onMouseDown={header.getResizeHandler()}
                        onTouchStart={header.getResizeHandler()}
                        className={cn(
                          "absolute right-0 top-0 h-full w-1 cursor-col-resize select-none touch-none",
                          header.column.getIsResizing() ? "bg-blue-500" : "bg-gray-200 opacity-0 hover:opacity-100"
                        )}
                      />
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows?.length ? (
                table.getRowModel().rows.map((row) => (
                  <TableRow
                    key={row.id}
                    data-state={row.getIsSelected() && "selected"}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <TableCell 
                        key={cell.id}
                        style={{
                          width: cell.column.getSize(),
                          position: cell.column.getIsPinned() ? 'sticky' : 'relative',
                          left: cell.column.getIsPinned() === 'left' ? `${cell.column.getStart()}px` : undefined,
                          background: cell.column.getIsPinned() ? 'white' : undefined,
                          zIndex: cell.column.getIsPinned() ? 1 : undefined,
                        }}
                      >
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={columns.length + 1} className="h-24 text-center">
                    No results.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  )
}