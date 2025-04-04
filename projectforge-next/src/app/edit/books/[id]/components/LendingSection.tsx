'use client'

import { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Calendar } from '@/components/ui/calendar'
import { UserPlus, RotateCcw } from 'lucide-react'
import { cn } from '@/lib/utils'

type LendingSectionProps = {
  isNewBook: boolean
  lendingInfo?: {
    lendOutBy?: string
    lendOutDate?: string
    lendOutDisplayName?: string
  }
  register: any
  setValue: any
  disabled: boolean
}

export default function LendingSection({ 
  isNewBook, 
  lendingInfo, 
  register, 
  setValue, 
  disabled 
}: LendingSectionProps) {
  const [date, setDate] = useState<Date | undefined>(
    lendingInfo?.lendOutDate ? new Date(lendingInfo.lendOutDate) : undefined
  )
  const [isLentOut, setIsLentOut] = useState<boolean>(!!lendingInfo?.lendOutBy)
  
  // Handle lending out a book
  const handleLendOut = () => {
    // This would typically open a user search dialog
    // For now, we'll just simulate selecting a user
    setIsLentOut(true)
    setValue('lendOutBy', 'user123')
    setValue('lendOutDisplayName', 'Jane Doe')
    setDate(new Date())
    setValue('lendOutDate', new Date().toISOString())
  }
  
  // Handle returning a book
  const handleReturn = () => {
    setIsLentOut(false)
    setValue('lendOutBy', null)
    setValue('lendOutDisplayName', null)
    setValue('lendOutDate', null)
    setDate(undefined)
  }
  
  if (isNewBook) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Lending</CardTitle>
          <CardDescription>
            Book lending will be available after creating the book.
          </CardDescription>
        </CardHeader>
      </Card>
    )
  }
  
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Lending Status</CardTitle>
        </CardHeader>
        <CardContent>
          {isLentOut ? (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 p-4 rounded-md bg-blue-50 border border-blue-200">
                <div>
                  <p className="font-medium">Currently lent out to:</p>
                  <p className="text-lg">{lendingInfo?.lendOutDisplayName || 'Jane Doe'}</p>
                  <p className="text-sm text-gray-500">
                    Since: {date ? date.toLocaleDateString() : 'N/A'}
                  </p>
                </div>
                <Button 
                  variant="outline" 
                  onClick={handleReturn}
                  disabled={disabled}
                  className="sm:self-start"
                >
                  <RotateCcw className="mr-2 h-4 w-4" />
                  Return Book
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 p-4 rounded-md bg-green-50 border border-green-200">
              <p className="font-medium">This book is currently available</p>
              <Button 
                onClick={handleLendOut} 
                disabled={disabled}
              >
                <UserPlus className="mr-2 h-4 w-4" />
                Lend Out
              </Button>
            </div>
          )}
          
          <div className="mt-6 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="lendOutDueDate">Due Date (Optional)</Label>
              <Calendar
                selected={date}
                onSelect={(newDate) => {
                  setDate(newDate)
                  setValue('lendOutDueDate', newDate?.toISOString())
                }}
                disabled={disabled}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="lendOutComment">Lending Notes</Label>
              <Textarea
                id="lendOutComment"
                {...register('lendOutComment')}
                placeholder="Add notes about this lending (optional)"
                className="resize-y min-h-[80px]"
                disabled={disabled}
              />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}