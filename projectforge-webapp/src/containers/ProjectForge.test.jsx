import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { applyMiddleware, createStore } from 'redux';
import { thunk } from 'redux-thunk';
import reducer from '../reducers';
import ProjectForge from './ProjectForge';

describe('renders without crashing', () => {
    it('with initial state', () => {
        const div = document.createElement('div');
        const store = createStore(reducer, applyMiddleware(thunk));

        createRoot(div).render(
            <Provider store={store}>
                <ProjectForge />
            </Provider>,
        );
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

        createRoot(div).render(
            <Provider store={store}>
                <ProjectForge />
            </Provider>,
        );
    });
});
