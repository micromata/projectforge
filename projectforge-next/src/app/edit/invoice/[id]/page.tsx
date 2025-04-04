'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useRouter } from 'next/navigation'
import {
  FormCard,
  TabsContainer,
  TextField,
  TextAreaField,
  SelectField,
  DatePickerField,
  SearchableSelectField,
  FormRow,
  FormColumn,
  FormSection,
} from '@/components/forms'
import InvoicePositions from './components/InvoicePositions'
import invoiceService from '@/services/invoiceService'
import { updateCsrfToken } from '@/services/api'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'

// Status options for invoice
const statusOptions = [
  { value: 'CREATED', label: 'Created' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'RECHNUNG', label: 'Invoice' },
  { value: 'GEPLANT', label: 'Planned' },
  { value: 'STORNIERT', label: 'Canceled' },
]

// Type options for invoice
const typeOptions = [
  { value: 'STANDARD', label: 'Standard' },
  { value: 'GUTSCHRIFT', label: 'Credit Note' },
  { value: 'ABSCHLAGSRECHNUNG', label: 'Advance Invoice' },
  { value: 'SCHLUSSRECHNUNG', label: 'Final Invoice' },
]

// Payment period options
const paymentPeriodOptions = [
  { value: '7', label: '7 days' },
  { value: '14', label: '14 days' },
  { value: '30', label: '30 days' },
  { value: '60', label: '60 days' },
  { value: '90', label: '90 days' },
]

export default function InvoiceEditPage() {
  const { id } = useParams()
  const router = useRouter()
  const [invoice, setInvoice] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const isNew = id === 'new'
  
  // Load invoice data
  const loadInvoice = useCallback(async () => {
    try {
      setIsLoading(true)
      
      if (isNew) {
        // For a new invoice, fetch the edit form which includes CSRF token and initial data
        try {
          const response = await invoiceService.getInvoiceWithLayout('new')
          if (response.success && response.data) {
            // Store the CSRF token
            if (response.data.serverData?.csrfToken) {
              updateCsrfToken(response.data.serverData.csrfToken);
              console.log('CSRF token received:', response.data.serverData.csrfToken);
            }
            
            // Set the invoice data
            setInvoice({
              ...response.data.data,
              status: 'CREATED',
              typ: 'STANDARD',
              positionen: response.data.data.positionen || [],
            })
            console.log('Initial invoice loaded with layout:', response.data)
          } else {
            // Fallback defaults
            setInvoice({
              status: 'CREATED',
              typ: 'STANDARD',
              positionen: [],
            })
          }
        } catch (err) {
          console.error('Error loading initial invoice structure:', err)
          // Fallback defaults
          setInvoice({
            status: 'CREATED',
            typ: 'STANDARD',
            positionen: [],
          })
        }
      } else {
        // Fetch existing invoice with layout data (includes CSRF token)
        try {
          const response = await invoiceService.getInvoiceWithLayout(id)
          if (response.success && response.data) {
            // Store the CSRF token
            if (response.data.serverData?.csrfToken) {
              updateCsrfToken(response.data.serverData.csrfToken);
              console.log('CSRF token received:', response.data.serverData.csrfToken);
            }
            
            // The data is in the data property of the layout response
            setInvoice(response.data.data)
            console.log('Invoice loaded with layout:', response.data)
          } else {
            throw new Error(response.error || 'Failed to load invoice')
          }
        } catch (err) {
          console.error('Error loading invoice:', err)
          // For development/testing purposes only
          setInvoice({
            id: id,
            nummer: Number(id),
            status: 'RECHNUNG',
            typ: 'STANDARD',
            betreff: 'Sample Invoice',
            bemerkung: 'This is a sample invoice',
            positionen: [],
            netSum: 0,
            vatAmountSum: 0,
            grossSum: 0,
          })
        }
      }
    } catch (err) {
      setError(err)
    } finally {
      setIsLoading(false)
    }
  }, [id, isNew])
  
  // Handle form submission
  const handleSubmit = useCallback(async () => {
    console.log('Saving invoice:', invoice)
    
    // Format the invoice data properly for the API
    const formattedInvoice = {
      ...invoice,
      // Ensure all date fields are correctly formatted as YYYY-MM-DD strings
      datum: invoice.datum ? (typeof invoice.datum === 'string' ? invoice.datum : new Date(invoice.datum).toISOString().split('T')[0]) : null,
      faelligkeit: invoice.faelligkeit ? (typeof invoice.faelligkeit === 'string' ? invoice.faelligkeit : new Date(invoice.faelligkeit).toISOString().split('T')[0]) : null,
      bezahlDatum: invoice.bezahlDatum ? (typeof invoice.bezahlDatum === 'string' ? invoice.bezahlDatum : new Date(invoice.bezahlDatum).toISOString().split('T')[0]) : null,
      discountMaturity: invoice.discountMaturity ? (typeof invoice.discountMaturity === 'string' ? invoice.discountMaturity : new Date(invoice.discountMaturity).toISOString().split('T')[0]) : null,
      periodOfPerformanceBegin: invoice.periodOfPerformanceBegin ? (typeof invoice.periodOfPerformanceBegin === 'string' ? invoice.periodOfPerformanceBegin : new Date(invoice.periodOfPerformanceBegin).toISOString().split('T')[0]) : null,
      periodOfPerformanceEnd: invoice.periodOfPerformanceEnd ? (typeof invoice.periodOfPerformanceEnd === 'string' ? invoice.periodOfPerformanceEnd : new Date(invoice.periodOfPerformanceEnd).toISOString().split('T')[0]) : null,
      // Ensure each position has proper data format
      positionen: invoice.positionen?.map(pos => {
        return {
          ...pos,
          // Make sure both text fields are synchronized
          text: pos.text || pos.s_text || '',
          s_text: pos.s_text || pos.text || ''
        };
      }),
    }
    
    // Call the API - using createInvoice for both new and existing invoices
    // since the backend doesn't support PUT requests
    try {
      // Always use POST (createInvoice) since the backend doesn't support PUT
      const response = await invoiceService.createInvoice(formattedInvoice);
      
      if (response.success) {
        router.push('/list/invoices')
      } else {
        // Check if we got a CSRF token in the error response
        if (response.data?.variables?.serverData?.csrfToken) {
          // Update the CSRF token and try again
          updateCsrfToken(response.data.variables.serverData.csrfToken);
          console.log('Updated CSRF token, retrying submission...');
          
          // Retry the submission with the new token
          const retryResponse = await invoiceService.createInvoice(formattedInvoice);
          if (retryResponse.success) {
            router.push('/list/invoices')
            return;
          }
        }
        
        // Handle validation errors
        if (response.data?.validationErrors) {
          const errorMessages = response.data.validationErrors
            .map(error => error.message)
            .join('\n');
          alert('Validation errors:\n' + errorMessages);
        } else {
          alert('Failed to save: ' + response.error);
        }
      }
    } catch (error) {
      console.error('Save error:', error)
      alert('Error saving invoice')
    }
  }, [invoice, router])
  
  // Handle delete
  const handleDelete = useCallback(async () => {
    try {
      const response = await invoiceService.deleteInvoice(invoice.id)
      
      if (response.success) {
        router.push('/list/invoices')
      } else {
        // Check if we got a CSRF token in the error response
        if (response.data?.variables?.serverData?.csrfToken) {
          // Update the CSRF token and try again
          updateCsrfToken(response.data.variables.serverData.csrfToken);
          console.log('Updated CSRF token, retrying deletion...');
          
          // Retry the deletion with the new token
          const retryResponse = await invoiceService.deleteInvoice(invoice.id);
          if (retryResponse.success) {
            router.push('/list/invoices')
            return;
          }
        }
        alert('Failed to delete: ' + response.error)
      }
    } catch (error) {
      console.error('Delete error:', error)
      alert('Error deleting invoice')
    } finally {
      setShowDeleteDialog(false)
    }
  }, [invoice?.id, router])
  
  // Handle invoice changes
  const handleInvoiceChange = useCallback((field, value) => {
    setInvoice(prev => ({
      ...prev,
      [field]: value
    }))
  }, [])
  
  // Handle positions update and recalculate totals
  const handlePositionsUpdate = useCallback((positions) => {
    // Calculate totals from positions
    const netSum = positions.reduce((sum, pos) => {
      const menge = isNaN(Number(pos.menge)) ? 0 : Number(pos.menge);
      const einzelNetto = isNaN(Number(pos.einzelNetto)) ? 0 : Number(pos.einzelNetto);
      return sum + (menge * einzelNetto);
    }, 0);
    
    const vatAmountSum = positions.reduce((sum, pos) => {
      const menge = isNaN(Number(pos.menge)) ? 0 : Number(pos.menge);
      const einzelNetto = isNaN(Number(pos.einzelNetto)) ? 0 : Number(pos.einzelNetto);
      const vat = isNaN(Number(pos.vat)) ? 0 : Number(pos.vat);
      const posNetSum = menge * einzelNetto;
      return sum + (posNetSum * (vat / 100));
    }, 0);
    
    const grossSum = netSum + vatAmountSum;
    
    // Make sure each position has properly formatted fields
    const positionenWithInvoiceRef = positions.map(pos => ({
      ...pos,
      // Make sure both text fields are synchronized
      text: pos.text || pos.s_text || '',
      s_text: pos.s_text || pos.text || ''
    }));
    
    setInvoice(prev => ({
      ...prev,
      positionen: positionenWithInvoiceRef,
      netSum,
      vatAmountSum,
      grossSum
    }));
  }, [invoice])
  
  useEffect(() => {
    loadInvoice()
  }, [loadInvoice])
  
  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error: {error.message}</div>
  if (!invoice) return <div>No invoice found</div>
  
  // Define the tab content
  const tabs = [
    {
      id: 'general',
      label: 'General Information',
      content: (
        <>
          <FormRow>
            <FormColumn>
              <TextField 
                label="Subject" 
                id="betreff" 
                value={invoice.betreff}
                onChange={(e) => handleInvoiceChange('betreff', e.target.value)}
                placeholder="Invoice subject" 
                required 
              />
              
              <TextField 
                label="Number" 
                id="nummer" 
                type="number" 
                value={invoice.nummer}
                onChange={(e) => handleInvoiceChange('nummer', e.target.value)}
                placeholder="Will be assigned automatically if empty"
              />
              
              <SelectField 
                label="Status" 
                id="status" 
                options={statusOptions} 
                value={invoice.status}
                onChange={(value) => handleInvoiceChange('status', value)}
                required
              />
              
              <SelectField 
                label="Type" 
                id="typ" 
                options={typeOptions} 
                value={invoice.typ}
                onChange={(value) => handleInvoiceChange('typ', value)}
                required
              />
            </FormColumn>
            
            <FormColumn>
              <SearchableSelectField 
                label="Project" 
                loadOptions={async (query) => {
                  try {
                    const response = await invoiceService.getProjects(query);
                    if (response.success && response.data) {
                      return response.data.map(project => ({
                        id: String(project.id),
                        label: project.displayName || project.name,
                        description: project.customer?.displayName || ''
                      }));
                    }
                    return [];
                  } catch (error) {
                    console.error('Error loading projects:', error);
                    return [];
                  }
                }}
                value={invoice.project ? {
                  id: String(invoice.project.id),
                  label: invoice.project.displayName
                } : null}
                onSelect={(project) => handleInvoiceChange('project', project ? {
                  id: Number(project.id),
                  displayName: project.label
                } : null)}
              />
              
              <SearchableSelectField 
                label="Customer" 
                loadOptions={async (query) => {
                  try {
                    const response = await invoiceService.getCustomers(query);
                    if (response.success && response.data) {
                      return response.data.map(customer => ({
                        id: String(customer.id),
                        label: customer.displayName || customer.name
                      }));
                    }
                    return [];
                  } catch (error) {
                    console.error('Error loading customers:', error);
                    return [];
                  }
                }}
                value={invoice.customer ? {
                  id: String(invoice.customer.id),
                  label: invoice.customer.displayName
                } : null}
                onSelect={(customer) => handleInvoiceChange('customer', customer ? {
                  id: Number(customer.id),
                  displayName: customer.label
                } : null)}
              />
              
              <TextAreaField 
                label="Customer Address" 
                id="customerAddress" 
                value={invoice.customerAddress}
                onChange={(e) => handleInvoiceChange('customerAddress', e.target.value)}
                placeholder="Enter customer address details" 
                rows={3}
              />
              
              <TextField 
                label="Customer Reference" 
                id="customerref1" 
                value={invoice.customerref1}
                onChange={(e) => handleInvoiceChange('customerref1', e.target.value)}
                placeholder="Customer reference number"
              />
            </FormColumn>
          </FormRow>
          
          <FormSection title="Period of Performance">
            <div className="flex flex-col sm:flex-row gap-4">
              <DatePickerField
                label="From"
                id="periodStart"
                value={invoice.periodOfPerformanceBegin ? new Date(invoice.periodOfPerformanceBegin) : null}
                onChange={(date) => handleInvoiceChange('periodOfPerformanceBegin', date ? date.toISOString().split('T')[0] : null)}
                className="flex-1"
              />
              
              <DatePickerField
                label="To"
                id="periodEnd"
                value={invoice.periodOfPerformanceEnd ? new Date(invoice.periodOfPerformanceEnd) : null}
                onChange={(date) => handleInvoiceChange('periodOfPerformanceEnd', date ? date.toISOString().split('T')[0] : null)}
                className="flex-1"
              />
            </div>
          </FormSection>
          
          <FormRow className="mt-6">
            <TextAreaField 
              label="Comments" 
              id="bemerkung" 
              value={invoice.bemerkung}
              onChange={(e) => handleInvoiceChange('bemerkung', e.target.value)}
              placeholder="Additional comments" 
              rows={4}
            />
            
            <TextAreaField 
              label="Special Notes" 
              id="besonderheiten" 
              value={invoice.besonderheiten}
              onChange={(e) => handleInvoiceChange('besonderheiten', e.target.value)}
              placeholder="Special considerations" 
              rows={4}
            />
          </FormRow>
        </>
      )
    },
    {
      id: 'positions',
      label: 'Invoice Positions',
      content: (
        <InvoicePositions 
          positions={invoice.positionen || []}
          onChange={handlePositionsUpdate} 
        />
      )
    },
    {
      id: 'payment',
      label: 'Payment Details',
      content: (
        <FormRow>
          <FormColumn>
            <DatePickerField
              label="Invoice Date"
              id="datum"
              value={invoice.datum ? new Date(invoice.datum) : null}
              onChange={(date) => handleInvoiceChange('datum', date ? date.toISOString().split('T')[0] : null)}
              required
            />
            
            <DatePickerField
              label="Due Date"
              id="faelligkeit"
              value={invoice.faelligkeit ? new Date(invoice.faelligkeit) : null}
              onChange={(date) => handleInvoiceChange('faelligkeit', date ? date.toISOString().split('T')[0] : null)}
            />
            
            <SelectField 
              label="Payment Period (days)" 
              id="zahlungsZielInTagen" 
              options={paymentPeriodOptions}
              value={invoice.zahlungsZielInTagen?.toString()}
              onChange={(value) => handleInvoiceChange('zahlungsZielInTagen', parseInt(value))}
            />
            
            <TextField 
              label="Discount (%)" 
              id="discountPercent" 
              type="number" 
              step="0.01"
              value={invoice.discountPercent}
              onChange={(e) => handleInvoiceChange('discountPercent', parseFloat(e.target.value))}
              placeholder="e.g. 2.00"
            />
          </FormColumn>
          
          <FormColumn>
            <div className="p-4 rounded-lg border bg-muted/50 space-y-4">
              <div className="grid grid-cols-2 gap-2">
                <div className="text-sm">
                  <div className="font-medium">Net sum:</div>
                  <div className="text-lg">€{invoice.netSum?.toFixed(2) || '0.00'}</div>
                </div>
                <div className="text-sm">
                  <div className="font-medium">VAT amount:</div>
                  <div className="text-lg">€{invoice.vatAmountSum?.toFixed(2) || '0.00'}</div>
                </div>
                <div className="text-sm">
                  <div className="font-medium">Gross sum:</div>
                  <div className="text-lg font-bold">€{invoice.grossSum?.toFixed(2) || '0.00'}</div>
                </div>
              </div>
            </div>
            
            <DatePickerField
              label="Payment Date"
              id="bezahlDatum"
              value={invoice.bezahlDatum ? new Date(invoice.bezahlDatum) : null}
              onChange={(date) => handleInvoiceChange('bezahlDatum', date ? date.toISOString().split('T')[0] : null)}
            />
            
            <TextField 
              label="Payment Amount" 
              id="zahlBetrag" 
              type="number" 
              step="0.01"
              value={invoice.zahlBetrag}
              onChange={(e) => handleInvoiceChange('zahlBetrag', parseFloat(e.target.value))}
              placeholder="e.g. 119.00"
            />
            
            <DatePickerField
              label="Discount Due Date"
              id="discountMaturity"
              value={invoice.discountMaturity ? new Date(invoice.discountMaturity) : null}
              onChange={(date) => handleInvoiceChange('discountMaturity', date ? date.toISOString().split('T')[0] : null)}
            />
          </FormColumn>
        </FormRow>
      )
    }
  ]

  return (
    <div className="container mx-auto py-6">
      <FormCard
        title={isNew ? 'Create Invoice' : `Edit Invoice #${invoice.nummer || id}`}
        description="Create or edit an invoice with client and project details"
        onCancel={() => router.push('/list/invoices')}
        onDelete={() => setShowDeleteDialog(true)}
        onSave={handleSubmit}
        isNew={isNew}
      >
        <TabsContainer tabs={tabs} defaultTab="general" />
      </FormCard>
      
      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure you want to delete this invoice?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete}>Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}