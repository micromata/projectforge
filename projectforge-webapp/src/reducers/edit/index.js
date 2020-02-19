import { combineReducers } from 'redux';
import categories from './categories';
import currentCategory from './currentCategory';

const reducer = combineReducers({
    currentCategory,
    categories,
});

export default reducer;
