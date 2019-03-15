import { combineReducers } from 'redux';
import authentication from './authentication';
import editPage from './editPage';


export default combineReducers({
    authentication,
    editPage,
});
