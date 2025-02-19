import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col } from '../../../design';
import { DynamicLayoutContext } from '../context';
import { buildLengthForColumn, lengthPropType } from './DynamicGroup';

// The Fieldset component enclosed in a col. Very similar to DynamicGroup.
function DynamicFieldset(props) {
    const {
        content,
        title,
        length,
        offset,
    } = props;

    // Get renderLayout function from context
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    // Render a Column around the fieldset and the enclosed layout.
    return React.useMemo(
        () => (
            <Col {...buildLengthForColumn(length, offset)}>
                <fieldset>
                    {title ? <legend>{title}</legend> : undefined}
                    {renderLayout(content)}
                </fieldset>
            </Col>
        ),
        [props],
    );
}

DynamicFieldset.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    title: PropTypes.string,
    length: lengthPropType,
    offset: lengthPropType,
};

export default DynamicFieldset;
