import React from 'react';
import ReactDOM from 'react-dom';
import Footer from '.';

describe('renders without crashing', () => {
    it('with default props', () => {
        const div = document.createElement('div');

        ReactDOM.render(<Footer version="0.0.0-TESTING" />, div);
    });

    it('with new version available', () => {
        const div = document.createElement('div');

        ReactDOM.render(<Footer version="0.0.0-TESTING" updateAvailable />, div);
    });
});
