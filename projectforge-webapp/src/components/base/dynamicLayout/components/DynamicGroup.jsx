import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col, FormGroup, Row } from '../../../design';
import { DynamicLayoutContext } from '../context';

// A Component to put a tag around dynamic layout content
function DynamicGroup(
    {
        content,
        length,
        type,
        smLength,
        mdLength,
        lgLength,
        xlLength,
    },
) {
    // Get renderLayout function from context.
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const groupProperties = {};

        // Determine the needed tag.
        let Tag;
        switch (type) {
            case 'COL':
                Tag = Col;
                groupProperties.xs = length;
                groupProperties.sm = smLength;
                groupProperties.md = mdLength;
                groupProperties.lg = lgLength;
                groupProperties.xl = xlLength;
                break;
            case 'FRAGMENT':
                Tag = React.Fragment;
                break;
            case 'GROUP':
                Tag = FormGroup;
                groupProperties.row = true;
                break;
            case 'ROW':
                Tag = Row;
                break;
            // When no type detected, use React.Fragment
            default:
                Tag = React.Fragment;
        }

        // Render tag and further content
        return (
            <Tag {...groupProperties}>
                {renderLayout(content)}
            </Tag>
        );
    }, [content, type]);
}

DynamicGroup.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    type: PropTypes.oneOf([
        // Supported Group Types
        'COL',
        'FRAGMENT',
        'GROUP',
        'ROW',
    ]).isRequired,
    length: PropTypes.number,
    smLength: PropTypes.number,
    mdLength: PropTypes.number,
    lgLength: PropTypes.number,
    xlLength: PropTypes.number,
};

DynamicGroup.defaultProps = {
    length: undefined,
    smLength: undefined,
    mdLength: undefined,
    lgLength: undefined,
    xlLength: undefined,
};

export default DynamicGroup;
