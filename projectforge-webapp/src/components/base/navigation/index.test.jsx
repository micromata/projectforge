import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter as Router } from 'react-router-dom';
import { cleanup, fireEvent, render, wait } from 'react-testing-library';
import Navigation from '.';

describe('renders without crashing', () => {
    it('with no props', () => {
        const div = document.createElement('div');

        ReactDOM.render((
            <Router>
                <Navigation />
            </Router>
        ), div);
    });

    it('with categories', () => {
        const div = document.createElement('div');
        const categories = [
            {
                name: 'General',
                items: [
                    {
                        name: 'Calendar',
                        url: '/calendar',
                    },
                    {
                        name: 'Calendar List',
                        url: '/calendarList',
                    },
                ],
            },
            {
                name: 'Project Management',
                items: [],
            },
        ];
        ReactDOM.render((
            <Router>
                <Navigation
                    categories={categories}
                />
            </Router>
        ), div);
    });

    it('with entries', () => {
        const div = document.createElement('div');
        const entries = [
            {
                name: 'Administration',
                items: [
                    {
                        name: 'User',
                        url: '/user',
                    },
                    {
                        name: 'Groups',
                        url: '/groups',
                    },
                ],
            },
            {
                name: 'Change Password',
                url: '/changePassword',
            },
        ];
        ReactDOM.render((
            <Router>
                <Navigation
                    entries={entries}
                />
            </Router>
        ), div);
    });
});

describe('interaction', () => {
    afterEach(cleanup);

    it('handle mobile navigation', () => {
        const { queryByLabelText } = render((
            <Router>
                <Navigation
                    entries={[{
                        name: 'Change Password',
                        url: '/changePassword',
                    }, {
                        name: 'Logout',
                        url: '/logout',
                    }]}
                />
            </Router>
        ));

        const toggler = queryByLabelText('toggleMobileNavbar');
        const collapse = queryByLabelText('navbar-collapse');

        expect(toggler)
            .toBeDefined();
        expect(collapse)
            .toBeDefined();
        expect(collapse.className)
            .not
            .toContain('show');

        fireEvent.click(toggler);

        // Need this wait, because reactstrap adds the show class after x ms.
        return wait(() => {
            expect(collapse.className)
                .toContain('show');
        });
    });
});
