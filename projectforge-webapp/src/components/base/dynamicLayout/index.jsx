import PropTypes from 'prop-types';
import React from 'react';
import DynamicPageMenu from './DynamicPageMenu';

function DynamicLayout({ ui }) {
    // Destructure the 'ui' prop.
    const {
        title,
        pageMenu,
    } = ui;

    // Set the document title when a title for the page is specified.
    if (title) {
        document.title = `ProjectForge - ${title}`;
    }

    return (
        <React.Fragment>
            <DynamicPageMenu menu={pageMenu} title={title} />
        </React.Fragment>
    );
}

DynamicLayout.propTypes = {
    ui: PropTypes.shape({}).isRequired,
};

export default DynamicLayout;
