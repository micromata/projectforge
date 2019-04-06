import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-big-calendar/lib/css/react-big-calendar.css'
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { applyMiddleware, createStore } from 'redux';
import thunk from 'redux-thunk';
import './assets/style/projectforge.scss';
import ProjectForge from './containers/ProjectForge';
import reducer from './reducers';
import * as serviceWorker from './serviceWorker';
import './utilities/global';

const store = createStore(reducer, applyMiddleware(thunk));

ReactDOM.render(
    /* eslint-disable-next-line react/jsx-filename-extension */
    <Provider store={store}>
        <ProjectForge />
    </Provider>,
    document.getElementById('root'),
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
