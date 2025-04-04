import api from './api'

export interface Customer {
  id: number
  name: string
  displayName: string
}

export interface Project {
  id: number
  name: string
  displayName: string
  customer?: Customer
}

export interface Konto {
  id: number
  nummer: string
  bezeichnung: string
  displayName: string
}

export interface InvoicePosition {
  id?: number
  number: number // This is the 1-based position number
  menge: number
  einzelNetto: number
  vat: number
  text: string
  s_text?: string // Used interchangeably with text
  periodOfPerformanceType: string
  periodOfPerformanceBegin?: string | null
  periodOfPerformanceEnd?: string | null
  auftragsPosition?: any
  netSum?: number
  vatAmount?: number
  grossSum?: number
  // We don't need to manually set this - the backend takes care of it
  // rechnung?: { id: number }
}

export interface Invoice {
  id?: number
  nummer?: number
  customer?: Customer
  kundeText?: string
  project?: Project
  status?: string
  typ?: string
  customerref1?: string
  attachment?: string
  customerAddress?: string
  periodOfPerformanceBegin?: string | null
  periodOfPerformanceEnd?: string | null
  datum?: string | null
  betreff?: string
  bemerkung?: string
  besonderheiten?: string
  faelligkeit?: string | null
  zahlungsZielInTagen?: number
  discountZahlungsZielInTagen?: number
  bezahlDatum?: string | null
  zahlBetrag?: number
  konto?: Konto
  discountPercent?: number
  discountMaturity?: string | null
  positionen?: InvoicePosition[]
  netSum?: number
  vatAmountSum?: number
  grossSum?: number
  grossSumWithDiscount?: number
}

const INVOICE_ENDPOINT = '/outgoingInvoice'

/**
 * Service for invoice API calls
 */
const invoiceService = {
  /**
   * Get a list of invoices
   */
  getInvoices: async (filter?: any) => {
    return api.post<{ resultSet: Invoice[]; totalSize: number }>(`${INVOICE_ENDPOINT}/list`, filter)
  },

  /**
   * Get a single invoice by ID
   */
  getInvoice: async (id: string | number) => {
    // Based on the backend code, the correct endpoint is:
    return api.get<Invoice>(`${INVOICE_ENDPOINT}/${id}`)
  },

  /**
   * Get a single invoice with layout data (includes CSRF token)
   */
  getInvoiceWithLayout: async (id: string | number) => {
    // Get the invoice with the edit layout data which includes the CSRF token
    return api.get<{ data: Invoice; ui: any; serverData: any }>(`${INVOICE_ENDPOINT}/edit?id=${id}`)
  },

  /**
   * Create a new invoice
   * Note: The backend expects PUT for save/update
   */
  createInvoice: async (invoice: Invoice) => {
    console.log('Sending invoice to backend:', JSON.stringify(invoice, null, 2));
    return api.put<Invoice>(`${INVOICE_ENDPOINT}/saveorupdate`, invoice)
  },

  /**
   * Update an existing invoice
   * Note: The backend expects PUT for save/update
   */
  updateInvoice: async (invoice: Invoice) => {
    return api.put<Invoice>(`${INVOICE_ENDPOINT}/saveorupdate`, invoice)
  },

  /**
   * Delete an invoice
   */
  deleteInvoice: async (id: string | number) => {
    return api.delete<void>(`${INVOICE_ENDPOINT}/markAsDeleted/${id}`)
  },

  /**
   * Get initial structure for creating a new invoice
   */
  getInitial: async () => {
    return api.get<Invoice>(`${INVOICE_ENDPOINT}/initial`)
  },

  /**
   * Get available customers for selection
   */
  getCustomers: async (query: string) => {
    return api.get<Customer[]>(`/customer/autosearch?search=${encodeURIComponent(query)}`)
  },

  /**
   * Get available projects for selection
   */
  getProjects: async (query: string) => {
    return api.get<Project[]>(`/project/autosearch?search=${encodeURIComponent(query)}`)
  },

  /**
   * Get available accounts for selection
   */
  getAccounts: async () => {
    return api.get<Konto[]>(`/account/list`)
  },
}

export default invoiceService
