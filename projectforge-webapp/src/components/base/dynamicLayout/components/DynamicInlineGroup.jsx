import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { DynamicLayoutContext } from '../context';

// A Component to display elements inline side-by-side
function DynamicInlineGroup(props) {
    const { content } = props;

    // Get renderLayout function from context.
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => (
        <div style={{
            display: 'inline-flex',
            gap: '0.5rem',
            alignItems: 'center',
            flexWrap: 'nowrap',
        }}
        >
            {renderLayout(content)}
        </div>
    ), [props]);
}

DynamicInlineGroup.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
};

export default DynamicInlineGroup;
