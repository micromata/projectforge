import React from 'react';
import ReactDOM from 'react-dom';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import 'bootstrap/dist/css/bootstrap.min.css';
import './assets/style/projectforge.scss';
import reducer from './reducers';
import ProjectForge from './containers/ProjectForge';
import * as serviceWorker from './serviceWorker';

const store = createStore(reducer);

ReactDOM.render(
    /* eslint-disable-next-line */
    <Provider store={store}>
        <ProjectForge />
    </Provider>,
    document.getElementById('root'),
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
