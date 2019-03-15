import React from 'react';
import ReactDOM from 'react-dom';
import { cleanup, fireEvent, render } from 'react-testing-library';
import LoginView from './LoginView';

describe('renders without crashing', () => {
    afterEach(cleanup);

    it('not loading', () => {
        const div = document.createElement('div');

        ReactDOM.render((
            <LoginView
                loading={false}
                login={jest.fn()}
            />
        ), div);
    });

    it('is loading', () => {
        const div = document.createElement('div');

        ReactDOM.render((
            <LoginView
                loading
                login={jest.fn()}
            />
        ), div);
    });
});

describe('interaction', () => {
    afterEach(cleanup);

    it('handle input change', () => {
        const { getByLabelText } = render(
            <LoginView loading={false} login={jest.fn()} />,
        );

        const username = getByLabelText('username');
        expect(username)
            .toBeDefined();
        expect(username.value)
            .toBe('');
        fireEvent.change(username, {
            target: {
                ...username,
                value: 'demo',
            },
        });
        expect(username.value)
            .toBe('demo');

        const password = getByLabelText('password');
        expect(password)
            .toBeDefined();
        expect(password.value)
            .toBe('');
        fireEvent.change(password, {
            target: {
                ...password,
                value: 'demo123',
            },
        });
        expect(password.value)
            .toBe('demo123');

        const checkbox = getByLabelText('keepSignedIn');
        expect(checkbox)
            .toBeDefined();
        expect(checkbox.checked)
            .toBe(false);
        fireEvent.click(checkbox);
        expect(checkbox.checked)
            .toBe(true);
    });

    it('handle form submit', () => {
        const loginFunction = jest.fn((username, password, keepSignedIn) => {
            expect(username)
                .toBe('demo');
            expect(password)
                .toBe('demo123');
            expect(keepSignedIn)
                .toBe(true);
        });

        const { getByLabelText } = render(
            <LoginView loading={false} login={loginFunction} />,
        );

        const login = getByLabelText('login');
        expect(login)
            .toBeDefined();

        // Inserting test values
        fireEvent.change(getByLabelText('username'), {
            target: {
                value: 'demo',
            },
        });
        fireEvent.change(getByLabelText('password'), {
            target: {
                value: 'demo123',
            },
        });
        fireEvent.click(getByLabelText('keepSignedIn'));

        expect(loginFunction.mock.calls.length)
            .toBe(0);
        fireEvent.click(login);
        expect(loginFunction.mock.calls.length)
            .toBe(1);
    });
});

describe('alerts', () => {
    afterEach(cleanup);

    it('administrator login needed', () => {
        const { queryByLabelText, rerender } = render(
            <LoginView
                loading={false}
                login={jest.fn()}
            />,
        );


        expect(queryByLabelText('administratorLoginNeededAlert'))
            .toBeNull();
        rerender(
            <LoginView
                loading={false}
                login={jest.fn()}
                administratorLoginNeeded
            />,
        );
        expect(queryByLabelText('administratorLoginNeededAlert'))
            .toBeDefined();


        expect(queryByLabelText('errorAlert'))
            .toBeNull();
        rerender(
            <LoginView
                loading={false}
                login={jest.fn()}
                error="Some uncool error message"
            />,
        );
        expect(queryByLabelText('errorAlert'))
            .toBeDefined();


        expect(queryByLabelText('motdAlert'))
            .toBeNull();
        rerender(
            <LoginView
                loading={false}
                login={jest.fn()}
                motd="Have a nice day :-)"
            />,
        );
        expect(queryByLabelText('motdAlert'))
            .toBeDefined();


        rerender(
            <LoginView
                loading={false}
                login={jest.fn()}
            />,
        );
        expect(queryByLabelText('administratorLoginNeededAlert'))
            .toBeNull();
        expect(queryByLabelText('errorAlert'))
            .toBeNull();
        expect(queryByLabelText('motdAlert'))
            .toBeNull();
    });
});
