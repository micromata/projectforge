import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col } from '../../../design';
import { DynamicLayoutContext } from '../context';

// The Fieldset component enclosed in a col. Very similar to DynamicGroup.
function DynamicFieldset({ content, title, length }) {
    // Get renderLayout function from context
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    // Render a Column around the fieldset and the enclosed layout.
    return React.useMemo(
        () => (
            <Col sm={length}>
                <fieldset>
                    {title ? <legend>{title}</legend> : undefined}
                    {renderLayout(content)}
                </fieldset>
            </Col>
        ),
        [content, title, length],
    );
}

DynamicFieldset.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    title: PropTypes.string,
    length: PropTypes.number,
};

DynamicFieldset.defaultProps = {
    title: undefined,
    length: undefined,
};

export default DynamicFieldset;
