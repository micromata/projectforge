import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col } from '../../../design';
import { DynamicLayoutContext } from '../context';

// The Fieldset component enclosed in a col. Very similar to DynamicGroup.
function DynamicFieldset({ content }) {
    // Get renderLayout function from context
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    // Render a Column around the fieldset and the enclosed layout.
    return (
        <Col>
            <fieldset>
                {renderLayout(content)}
            </fieldset>
        </Col>
    );
}

DynamicFieldset.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
};

DynamicFieldset.defaultProps = {};

export default DynamicFieldset;
