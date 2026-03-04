import { library } from '@fortawesome/fontawesome-svg-core';
import { far } from '@fortawesome/free-regular-svg-icons';
import { fas } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { Provider } from 'react-redux';
import { applyMiddleware, compose, createStore } from 'redux';
import { thunk } from 'redux-thunk';
import './assets/style/projectforge.scss';
import { createRoot } from 'react-dom/client';
import ProjectForge from './containers/ProjectForge';
import reducer from './reducers';
import * as serviceWorker from './serviceWorker';
import './utilities/global';
import CustomRouter from './containers/CustomRouter';

const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;
const store = createStore(reducer, /* preloadedState, */ composeEnhancers(
    applyMiddleware(thunk),
));

library.add(fas, far);

createRoot(document.getElementById('root')).render(
    /* eslint-disable-next-line react/jsx-filename-extension */
    <Provider store={store}>
        <CustomRouter>
            <ProjectForge />
        </CustomRouter>
    </Provider>,
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
