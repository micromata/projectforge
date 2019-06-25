import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col, Row } from '../../../design';
import { DynamicLayoutContext } from '../context';

// A Component to put a tag around dynamic layout content
function DynamicGroup({ content, type }) {
    // Get renderLayout function from context.
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        // Determine the needed tag.
        let Tag;
        switch (type) {
            case 'ROW':
                Tag = Row;
                break;
            case 'COL':
                Tag = Col;
                break;
            case 'FRAGMENT':
                Tag = React.Fragment;
                break;
            // When no type detected, use React.Fragment
            default:
                Tag = React.Fragment;
        }

        // Render tag and further content
        return <Tag>{renderLayout(content)}</Tag>;
    }, [content, type]);
}

DynamicGroup.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    type: PropTypes.oneOf([
        // Supported Group Types
        'ROW',
        'COL',
        'FRAGMENT',
    ]).isRequired,
};

export default DynamicGroup;
