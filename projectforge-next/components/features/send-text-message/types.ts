/** Contract of org.projectforge.rest.SendTextMessageRest. */

/** Deep-link parameters that prefill the receiver (see the SMS icon on an address's phone number). */
export interface SendTextMessageParams {
  addressId?: number;
  phoneType?: string;
  number?: string;
}

/** Initial form data: SendTextMessageRest.InitialData. */
export interface SendTextMessageInitialData {
  phoneNumber?: string | null;
  message: string;
  maxMessageSize: number;
  smsConfigured: boolean;
}

/** Body of the send call: SendTextMessageRest.SendRequest. */
export interface SendTextMessageRequest {
  phoneNumber: string;
  message: string;
}

/** Result of the send call: SendTextMessageRest.SendResult (`message` is already localized). */
export interface SendTextMessageResult {
  success: boolean;
  message: string;
}
