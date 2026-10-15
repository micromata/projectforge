/** Contract of org.projectforge.rest.FeedbackPageRest. */

/** Initial form data: FeedbackPageRest.InitialData (receiver and sender are read-only). */
export interface FeedbackInitialData {
  receiver?: string | null;
  sender?: string | null;
}

/** Body of the send call: FeedbackPageRest.SendRequest (only the description is the user's). */
export interface FeedbackRequest {
  description: string;
}

/** Result of the send call: FeedbackPageRest.SendResult (`message` is already localized). */
export interface FeedbackResult {
  success: boolean;
  message: string;
}
