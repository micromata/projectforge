import { callEndpointWithData } from '../editPage';

export const returnBook = () => (dispatch, getState) => callEndpointWithData(
    'books',
    'returnBook',
    getState().editPage.data,
    dispatch,
);

export const lendOutBook = () => (dispatch, getState) => callEndpointWithData(
    'books',
    'lendOut',
    getState().editPage.data,
    dispatch,
);
