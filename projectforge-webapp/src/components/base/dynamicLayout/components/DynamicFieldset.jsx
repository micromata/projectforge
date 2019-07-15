import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col } from '../../../design';
import { DynamicLayoutContext } from '../context';

// The Fieldset component enclosed in a col. Very similar to DynamicGroup.
function DynamicFieldset(props) {
    const {
        content,
        title,
        length,
        smLength,
        mdLength,
        lgLength,
        xlLength,
    } = props;

    // Get renderLayout function from context
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    // Render a Column around the fieldset and the enclosed layout.
    return React.useMemo(
        () => (
            <Col xs={length} sm={smLength} md={mdLength} lg={lgLength} xl={{ xlLength }}>
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
    length: PropTypes.number,
    smLength: PropTypes.number,
    mdLength: PropTypes.number,
    lgLength: PropTypes.number,
    xlLength: PropTypes.number,
};

DynamicFieldset.defaultProps = {
    title: undefined,
    length: undefined,
    smLength: undefined,
    mdLength: undefined,
    lgLength: undefined,
    xlLength: undefined,
};

export default DynamicFieldset;
