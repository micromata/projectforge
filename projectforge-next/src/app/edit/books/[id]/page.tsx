'use client'

import { useParams, useRouter } from 'next/navigation'
import { useState, useEffect, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Save, Trash2 } from 'lucide-react'
import BookForm from './components/BookForm'
import AttachmentList from './components/AttachmentList'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'

export default function BookEditPage() {
  const { id } = useParams()
  const router = useRouter()
  const [book, setBook] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isSaving, setIsSaving] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const isNewBook = id === 'new'
  
  // Load book data
  const loadBook = useCallback(async () => {
    try {
      setIsLoading(true)
      
      if (isNewBook) {
        // Create new book with default values
        setBook({
          title: '',
          authors: '',
          yearOfPublishing: '',
          publisher: '',
          isbn: '',
          keywords: '',
          signature: '',
          status: 'PRESENT',
          type: 'BOOK',
          editor: '',
          abstractText: '',
          comment: '',
          lendOutComment: '',
          attachments: []
        })
      } else {
        // In production, this would be a fetch call to your API
        // const response = await fetch(`/api/books/${id}`)
        // const result = await response.json()
        
        // For now, use the example data
        try {
          const result = await import('../exampleBooksEdit.json')
          setBook(result.default.data)
        } catch (err) {
          console.error('Error loading example data:', err)
          // Fallback mock data
          setBook({
            id: Number(id),
            title: 'Sample Book',
            authors: 'Sample Author',
            yearOfPublishing: '2023',
            publisher: 'Sample Publisher',
            isbn: '978-3-16-148410-0',
            keywords: 'sample, book, test',
            signature: 'SAMPLE-123',
            status: 'PRESENT',
            type: 'BOOK',
            editor: 'Sample Editor',
            abstractText: 'This is a sample book abstract.',
            comment: 'Sample comment.',
            lendOutComment: '',
            attachments: []
          })
        }
      }
      
      setError(null)
    } catch (err) {
      setError(err)
      setBook(null)
    } finally {
      setIsLoading(false)
    }
  }, [id, isNewBook])
  
  // Handle form submission
  const handleSubmit = useCallback(async (formData) => {
    try {
      setIsSaving(true)
      // In production, this would submit to your API
      console.log('Submitting book data:', formData)
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 800))
      
      // Success, go back to list
      router.push('/list/books')
    } catch (err) {
      setError(err)
    } finally {
      setIsSaving(false)
    }
  }, [router])
  
  // Handle delete
  const handleDelete = useCallback(async () => {
    try {
      setIsDeleting(true)
      // In production, this would call your API
      console.log('Deleting book:', id)
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 800))
      
      // Success, go back to list
      router.push('/list/books')
    } catch (err) {
      setError(err)
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }, [id, router])
  
  // Handle back/cancel
  const handleBack = useCallback(() => {
    router.push('/list/books')
  }, [router])
  
  useEffect(() => {
    loadBook()
  }, [loadBook])

  if (isLoading) return (
    <div className="flex justify-center items-center min-h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
    </div>
  )
  
  if (error) return (
    <div className="flex justify-center items-center min-h-screen">
      <div className="bg-red-50 p-6 rounded-md text-red-600">
        <h3 className="text-lg font-medium mb-2">Error loading book data</h3>
        <p>Please try again or contact support if the issue persists.</p>
        <Button 
          onClick={() => router.push('/list/books')}
          className="mt-4"
          variant="outline"
        >
          Return to Book List
        </Button>
      </div>
    </div>
  )
  
  if (!book) return (
    <div className="flex justify-center items-center min-h-screen">
      <div className="bg-amber-50 p-6 rounded-md text-amber-700">
        <h3 className="text-lg font-medium mb-2">No book found</h3>
        <p>The requested book could not be found.</p>
        <Button 
          onClick={() => router.push('/list/books')}
          className="mt-4"
          variant="outline"
        >
          Return to Book List
        </Button>
      </div>
    </div>
  )

  return (
    <div className="container mx-auto p-4 max-w-5xl">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-3xl font-bold">
          {isNewBook ? 'Add New Book' : `Edit Book: ${book.title}`}
        </h1>
        
        <div className="flex gap-2 self-stretch sm:self-auto">
          <Button 
            variant="outline" 
            onClick={handleBack}
            disabled={isSaving || isDeleting}
            className="flex-1 sm:flex-none"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Button>
          
          {!isNewBook && (
            <Button 
              variant="destructive"
              onClick={() => setShowDeleteDialog(true)}
              disabled={isSaving || isDeleting}
              className="flex-1 sm:flex-none"
            >
              {isDeleting ? (
                <span className="flex items-center">
                  <span className="animate-spin mr-2 h-4 w-4 border-t-2 border-b-2 border-white rounded-full"></span>
                  Deleting...
                </span>
              ) : (
                <span className="flex items-center">
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </span>
              )}
            </Button>
          )}
          
          <Button 
            type="submit"
            form="book-form"
            disabled={isSaving || isDeleting}
            className="flex-1 sm:flex-none"
          >
            {isSaving ? (
              <span className="flex items-center">
                <span className="animate-spin mr-2 h-4 w-4 border-t-2 border-b-2 border-white rounded-full"></span>
                Saving...
              </span>
            ) : (
              <span className="flex items-center">
                <Save className="mr-2 h-4 w-4" />
                Save
              </span>
            )}
          </Button>
        </div>
      </div>
      
      <div className="bg-white rounded-lg shadow-md">
        <BookForm 
          book={book} 
          onSubmit={handleSubmit} 
          isSaving={isSaving}
          isNewBook={isNewBook}
        />
      </div>
      
      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this book?</AlertDialogTitle>
            <AlertDialogDescription>
              This action will mark the book as deleted and cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} disabled={isDeleting}>
              {isDeleting ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}