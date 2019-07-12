import { combineReducers } from 'redux';
import authentication from './authentication';
import menu from './menu';


export default combineReducers({
    authentication,
    menu,
});
