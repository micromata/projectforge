import { callEndpointWithData } from '../editPage';

export const returnBook = () => callEndpointWithData('returnBook');

export const lendOutBook = () => callEndpointWithData('lendOut');
