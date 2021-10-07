import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { applyMiddleware, createStore } from 'redux';
import thunk from 'redux-thunk';
import reducer from '../reducers';
import ProjectForge from './ProjectForge';

describe('renders without crashing', () => {
    it('with initial state', () => {
        const div = document.createElement('div');
        const store = createStore(reducer, applyMiddleware(thunk));

        ReactDOM.render((
            <Provider store={store}>
                <ProjectForge />
            </Provider>
        ), div);
    });

    it('with logged in state', () => {
        const div = document.createElement('div');
        const store = createStore(
            reducer,
            {
                authentication: {
                    loading: false,
                    error: null,
                    loggedIn: true,
                },
            },
            applyMiddleware(thunk),
        );

        ReactDOM.render((
            <Provider store={store}>
                <ProjectForge />
            </Provider>
        ), div);
    });
});
