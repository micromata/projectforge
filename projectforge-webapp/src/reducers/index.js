import { combineReducers } from 'redux';
import authentication from './authentication';
import editPage from './editPage';
import listPage from './listPage';
import menu from './menu';


export default combineReducers({
    authentication,
    editPage,
    listPage,
    menu,
});
