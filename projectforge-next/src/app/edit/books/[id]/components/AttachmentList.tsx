'use client'

import { useCallback, useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Check, FileText, Download, MoreHorizontal, Paperclip, Trash2, Upload, X } from 'lucide-react'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

type Attachment = {
  id: number
  name: string
  description?: string
  size?: number
  sizeHumanReadable?: string
  created?: string
  createdFormatted?: string
  createdByUser?: string
  lastUpdate?: string
  lastUpdateTimeAgo?: string
  lastUpdateByUser?: string
}

type AttachmentListProps = {
  attachments: Attachment[]
  isNewBook: boolean
  disabled: boolean
}

export default function AttachmentList({ attachments, isNewBook, disabled }: AttachmentListProps) {
  const [files, setFiles] = useState<Attachment[]>(attachments || [])
  const [isUploading, setIsUploading] = useState(false)
  const [newAttachment, setNewAttachment] = useState<{file?: File, description: string}>({
    description: ''
  })
  const [showAddDialog, setShowAddDialog] = useState(false)
  
  // Handle file selection
  const handleFileChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0]
      setNewAttachment(prev => ({
        ...prev,
        file
      }))
    }
  }, [])
  
  // Handle file upload
  const handleUpload = useCallback(async () => {
    if (!newAttachment.file) return
    
    try {
      setIsUploading(true)
      
      // In production, this would upload to your API
      // For now, just simulate an upload
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      // Add to list
      const newFile: Attachment = {
        id: Date.now(),
        name: newAttachment.file.name,
        description: newAttachment.description,
        size: newAttachment.file.size,
        sizeHumanReadable: formatFileSize(newAttachment.file.size),
        created: new Date().toISOString(),
        createdFormatted: new Date().toLocaleDateString(),
        createdByUser: 'Current User',
        lastUpdate: new Date().toISOString(),
        lastUpdateTimeAgo: 'just now',
        lastUpdateByUser: 'Current User'
      }
      
      setFiles(prev => [...prev, newFile])
      setNewAttachment({ description: '' })
      setShowAddDialog(false)
    } catch (error) {
      console.error('Upload error:', error)
    } finally {
      setIsUploading(false)
    }
  }, [newAttachment])
  
  // Handle file deletion
  const handleDelete = useCallback((id: number) => {
    setFiles(prev => prev.filter(file => file.id !== id))
  }, [])
  
  // Format file size
  function formatFileSize(bytes?: number): string {
    if (bytes === undefined) return '-'
    if (bytes === 0) return '0 Bytes'
    
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }
  
  if (isNewBook) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Attachments</CardTitle>
          <CardDescription>
            File attachments will be available after creating the book.
          </CardDescription>
        </CardHeader>
      </Card>
    )
  }
  
  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-medium">Attachments</h3>
        <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
          <DialogTrigger asChild>
            <Button disabled={disabled}>
              <Paperclip className="mr-2 h-4 w-4" />
              Add Attachment
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add New Attachment</DialogTitle>
              <DialogDescription>
                Upload a file to attach to this book.
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4 py-4">
              <div className="flex flex-col items-center justify-center w-full">
                <label htmlFor="file-upload" className="w-full cursor-pointer">
                  <div className="flex flex-col items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-lg bg-gray-50 hover:bg-gray-100">
                    <div className="flex flex-col items-center justify-center pt-5 pb-6">
                      <Upload className="w-8 h-8 mb-2 text-gray-500" />
                      <p className="mb-2 text-sm text-gray-500">
                        <span className="font-semibold">Click to upload</span> or drag and drop
                      </p>
                      <p className="text-xs text-gray-500">
                        Any file type (max. 100MB)
                      </p>
                    </div>
                    {newAttachment.file && (
                      <div className="w-full px-4 py-2 bg-blue-50 border-t border-blue-100 flex justify-between items-center">
                        <div className="flex items-center">
                          <FileText className="h-4 w-4 mr-2 text-blue-500" />
                          <span className="text-sm truncate max-w-[200px]">
                            {newAttachment.file.name}
                          </span>
                        </div>
                        <span className="text-xs text-gray-500">
                          {formatFileSize(newAttachment.file.size)}
                        </span>
                      </div>
                    )}
                  </div>
                  <input 
                    id="file-upload" 
                    type="file" 
                    className="hidden" 
                    onChange={handleFileChange} 
                    disabled={isUploading}
                  />
                </label>
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="description">Description (Optional)</Label>
                <Textarea
                  id="description"
                  value={newAttachment.description}
                  onChange={(e) => setNewAttachment(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="Enter a description for this file"
                  className="resize-none"
                  disabled={isUploading}
                />
              </div>
            </div>
            
            <DialogFooter>
              <Button 
                variant="outline" 
                onClick={() => setShowAddDialog(false)}
                disabled={isUploading}
              >
                Cancel
              </Button>
              <Button 
                onClick={handleUpload} 
                disabled={!newAttachment.file || isUploading}
              >
                {isUploading ? (
                  <span className="flex items-center">
                    <span className="animate-spin mr-2 h-4 w-4 border-t-2 border-b-2 border-white rounded-full"></span>
                    Uploading...
                  </span>
                ) : (
                  <span className="flex items-center">
                    <Check className="mr-2 h-4 w-4" />
                    Upload
                  </span>
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
      
      {files.length === 0 ? (
        <Card>
          <CardContent className="p-6">
            <div className="flex flex-col items-center justify-center text-center p-4">
              <Paperclip className="h-8 w-8 text-gray-400 mb-2" />
              <h3 className="text-lg font-medium">No Attachments</h3>
              <p className="text-gray-500 max-w-md">
                There are no files attached to this book. Add attachments using the button above.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="border rounded-md">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Filename</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Size</TableHead>
                <TableHead>Created</TableHead>
                <TableHead>Created By</TableHead>
                <TableHead className="w-[80px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {files.map((file) => (
                <TableRow key={file.id}>
                  <TableCell className="font-medium flex items-center">
                    <FileText className="h-4 w-4 mr-2 text-blue-500" />
                    <span className="truncate max-w-[200px]">{file.name}</span>
                  </TableCell>
                  <TableCell className="text-gray-500 truncate max-w-[200px]">
                    {file.description || '–'}
                  </TableCell>
                  <TableCell>{file.sizeHumanReadable || formatFileSize(file.size)}</TableCell>
                  <TableCell>{file.createdFormatted || new Date(file.created || '').toLocaleDateString()}</TableCell>
                  <TableCell>{file.createdByUser || '–'}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          className="cursor-pointer flex items-center"
                          disabled={disabled}
                        >
                          <Download className="mr-2 h-4 w-4" />
                          <span>Download</span>
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="cursor-pointer flex items-center text-red-600"
                          onClick={() => handleDelete(file.id)}
                          disabled={disabled}
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          <span>Delete</span>
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}