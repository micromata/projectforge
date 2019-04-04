import { callEndpointWithData } from '../editPage';

export const returnBook = () => (dispatch, getState) => callEndpointWithData(
    'book',
    'returnBook',
    getState().editPage.data,
    dispatch,
);

export const lendOutBook = () => (dispatch, getState) => callEndpointWithData(
    'book',
    'lendOut',
    getState().editPage.data,
    dispatch,
);
