import React from 'react';
import { createRoot } from 'react-dom/client';
import Footer from '.';

describe('renders without crashing', () => {
    it('with default props', () => {
        const div = document.createElement('div');

        createRoot(div).render(<Footer version="0.0.0-TESTING" />);
    });

    it('with new version available', () => {
        const div = document.createElement('div');

        createRoot(div).render(<Footer version="0.0.0-TESTING" updateAvailable />);
    });
});
