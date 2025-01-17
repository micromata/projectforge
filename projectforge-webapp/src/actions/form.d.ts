// Action Types
export const FORM_CALL_ACTION_BEGIN: 'FORM_CALL_ACTION_BEGIN';
export const FORM_CALL_ACTION_SUCCESS: 'FORM_CALL_ACTION_SUCCESS';
export const FORM_CALL_FAILURE: 'FORM_CALL_FAILURE';
export const FORM_CALL_INITIAL_BEGIN: 'FORM_CALL_INITIAL_BEGIN';
export const FORM_CALL_SUCCESS: 'FORM_CALL_SUCCESS';
export const FORM_CHANGE_DATA: 'FORM_CHANGE_DATA';
export const FORM_CHANGE_VARIABLES: 'FORM_CHANGE_VARIABLES';
export const FORM_SWITCH_CATEGORY: 'FORM_SWITCH_CATEGORY';

// State interfaces
interface FormCategory {
    isFetching?: boolean;
    data?: any;
    serverData?: any;
    validationErrors?: any;
    ui?: {
        watchFields?: string[];
    };
}

interface FormState {
    currentCategory: string;
    categories: {
        [key: string]: FormCategory;
    };
}

// Action interfaces
interface ResponseAction {
    message?: {
        message: string;
        color: string;
    };
    targetType: 'REDIRECT' | 'MODAL' | 'CLOSE_MODAL' | 'UPDATE' | 'CHECK_AUTHENTICATION' |
        'DOWNLOAD' | 'NOTHING' | 'TOAST' | 'DELETE' | 'POST' | 'GET' | 'PUT';
    url?: string;
    variables?: any;
    merge?: boolean;
    absolute?: boolean;
    myData?: any;
}

// Action Parameters
interface CallActionParams {
    responseAction: ResponseAction;
    watchFieldsTriggered?: string[];
}

// Thunk Types
type GetState = () => { form: FormState };
type Dispatch = (action: any) => any;
type ThunkAction<R = void> = (dispatch: Dispatch, getState: GetState) => R;

// Exported Functions
export function switchFromCurrentCategory(
    to: string,
    newVariables?: any,
    merge?: boolean
): ThunkAction;

export function callAction(params: CallActionParams): ThunkAction<Promise<void>>;

export function loadFormPage(
    category: string,
    id: string | number,
    url: string,
    params?: Record<string, any>
): ThunkAction<Promise<void>>;

export function setCurrentData(newData: any): ThunkAction;

export function setCurrentVariables(newVariables: any): ThunkAction;
