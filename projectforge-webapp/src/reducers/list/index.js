import { combineReducers } from 'redux';
import currentCategory from './currentCategory';
import categories from './categories';

const reducer = combineReducers({
    currentCategory,
    categories,
});

export default reducer;
