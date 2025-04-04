'use client'

import { useForm } from 'react-hook-form'
import { useCallback, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import AttachmentList from './AttachmentList'
import LendingSection from './LendingSection'

type BookFormProps = {
  book: any
  onSubmit: (data: any) => void
  isSaving: boolean
  isNewBook: boolean
}

export default function BookForm({ book, onSubmit, isSaving, isNewBook }: BookFormProps) {
  const { register, handleSubmit, formState: { errors }, setValue, control, watch } = useForm({
    defaultValues: {
      ...book
    }
  })
  
  // Update form when book data changes
  useEffect(() => {
    Object.entries(book).forEach(([key, value]) => {
      setValue(key, value)
    })
  }, [book, setValue])
  
  // Handle form submission
  const onSubmitHandler = useCallback((data) => {
    onSubmit(data)
  }, [onSubmit])
  
  return (
    <form id="book-form" onSubmit={handleSubmit(onSubmitHandler)}>
      <Tabs defaultValue="basic" className="w-full">
        <TabsList className="w-full justify-start border-b rounded-none">
          <TabsTrigger value="basic">Basic Information</TabsTrigger>
          <TabsTrigger value="details">Details</TabsTrigger>
          <TabsTrigger value="lending">Lending</TabsTrigger>
          <TabsTrigger value="attachments">Attachments</TabsTrigger>
        </TabsList>
        
        {/* Basic Information Tab */}
        <TabsContent value="basic" className="p-6">
          <div className="grid grid-cols-1 gap-6">
            {/* Title */}
            <div className="space-y-2">
              <Label htmlFor="title" className="required">Title</Label>
              <Input
                id="title"
                {...register('title', { required: 'Title is required' })}
                placeholder="Enter book title"
                className={errors.title ? 'border-red-500' : ''}
                disabled={isSaving}
                autoFocus
              />
              {errors.title && (
                <p className="text-red-500 text-sm">{errors.title.message}</p>
              )}
            </div>
            
            {/* Authors */}
            <div className="space-y-2">
              <Label htmlFor="authors">Authors</Label>
              <Textarea
                id="authors"
                {...register('authors')}
                placeholder="Enter authors (one per line or comma-separated)"
                className="min-h-[70px] resize-y"
                disabled={isSaving}
              />
            </div>
            
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="type">Type</Label>
                <Select
                  defaultValue={book.type || 'BOOK'}
                  onValueChange={(value) => setValue('type', value)}
                  disabled={isSaving}
                >
                  <SelectTrigger id="type">
                    <SelectValue placeholder="Select book type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="AUDIO_BOOK">Audio Book</SelectItem>
                    <SelectItem value="BOOK">Book</SelectItem>
                    <SelectItem value="EBOOK">E-Book</SelectItem>
                    <SelectItem value="MAGAZINE">Magazine</SelectItem>
                    <SelectItem value="ARTICLE">Article</SelectItem>
                    <SelectItem value="NEWSPAPER">Newspaper</SelectItem>
                    <SelectItem value="PERIODICAL">Periodical</SelectItem>
                    <SelectItem value="FILM">Film (data medium)</SelectItem>
                    <SelectItem value="SOFTWARE">Software (data medium)</SelectItem>
                    <SelectItem value="THESIS">Thesis</SelectItem>
                    <SelectItem value="MISC">Misc</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="status" className="required">Status</Label>
                <Select
                  defaultValue={book.status || 'PRESENT'}
                  onValueChange={(value) => setValue('status', value)}
                  disabled={isSaving}
                >
                  <SelectTrigger id="status">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PRESENT">Present</SelectItem>
                    <SelectItem value="MISSED">Missed</SelectItem>
                    <SelectItem value="DISPOSED">Disposed</SelectItem>
                    <SelectItem value="UNKNOWN">Unknown</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="yearOfPublishing">Year of Publishing</Label>
                <Input
                  id="yearOfPublishing"
                  {...register('yearOfPublishing', { 
                    maxLength: { value: 4, message: 'Year must be 4 digits or less' },
                    pattern: { value: /^\d*$/, message: 'Year must contain only digits' }
                  })}
                  placeholder="YYYY"
                  maxLength={4}
                  disabled={isSaving}
                />
                {errors.yearOfPublishing && (
                  <p className="text-red-500 text-sm">{errors.yearOfPublishing.message}</p>
                )}
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="signature">Signature</Label>
                <Input
                  id="signature"
                  {...register('signature')}
                  placeholder="Enter signature"
                  disabled={isSaving}
                />
              </div>
            </div>
          </div>
        </TabsContent>
        
        {/* Details Tab */}
        <TabsContent value="details" className="p-6">
          <div className="grid grid-cols-1 gap-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="isbn">ISBN</Label>
                <Input
                  id="isbn"
                  {...register('isbn')}
                  placeholder="ISBN"
                  disabled={isSaving}
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="publisher">Publisher</Label>
                <Input
                  id="publisher"
                  {...register('publisher')}
                  placeholder="Publisher name"
                  disabled={isSaving}
                />
              </div>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="editor">Editor</Label>
              <Input
                id="editor"
                {...register('editor')}
                placeholder="Editor name"
                disabled={isSaving}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="keywords">Keywords</Label>
              <Textarea
                id="keywords"
                {...register('keywords')}
                placeholder="Enter keywords (comma-separated)"
                className="resize-y min-h-[80px]"
                disabled={isSaving}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="abstractText">Abstract</Label>
              <Textarea
                id="abstractText"
                {...register('abstractText')}
                placeholder="Book abstract"
                className="min-h-[120px] resize-y"
                disabled={isSaving}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="comment">Comment</Label>
              <Textarea
                id="comment"
                {...register('comment')}
                placeholder="Additional comments"
                className="min-h-[120px] resize-y"
                disabled={isSaving}
              />
            </div>
          </div>
        </TabsContent>
        
        {/* Lending Tab */}
        <TabsContent value="lending" className="p-6">
          <LendingSection
            isNewBook={isNewBook}
            lendingInfo={book.lendOutInfo}
            register={register}
            setValue={setValue}
            disabled={isSaving}
          />
        </TabsContent>
        
        {/* Attachments Tab */}
        <TabsContent value="attachments" className="p-6">
          <AttachmentList
            attachments={book.attachments || []}
            isNewBook={isNewBook}
            disabled={isSaving}
          />
        </TabsContent>
      </Tabs>
    </form>
  )
}