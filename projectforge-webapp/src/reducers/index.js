import { combineReducers } from 'redux';
import authentication from './authentication';
import form from './form';
import list from './list';
import menu from './menu';


export default combineReducers({
    authentication,
    menu,
    list,
    form,
});
