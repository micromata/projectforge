import { combineReducers } from 'redux';
import authentication from './authentication';
import edit from './edit';
import list from './list';
import menu from './menu';


export default combineReducers({
    authentication,
    menu,
    list,
    edit,
});
