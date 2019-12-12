import { combineReducers } from 'redux';
import authentication from './authentication';
import menu from './menu';
import list from './list';


export default combineReducers({
    authentication,
    menu,
    list,
});
