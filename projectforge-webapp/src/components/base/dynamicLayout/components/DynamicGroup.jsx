import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col, FormGroup, Row } from '../../../design';
import { DynamicLayoutContext } from '../context';

// A Component to put a tag around dynamic layout content
function DynamicGroup(props) {
    const {
        content,
        length,
        offset,
        type,
    } = props;

    // Get renderLayout function from context.
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        let groupProperties = {};

        // Determine the needed tag.
        let Tag;
        switch (type) {
            case 'COL':
                Tag = Col;

                if (length) {
                    groupProperties = {
                        ...groupProperties,
                        ...(offset
                            ? Object.keys(length)
                                .reduce((previousValue, key) => ({
                                    ...previousValue,
                                    [key]: {
                                        size: length[key],
                                        offset: offset[key],
                                    },
                                }), {})
                            : length),
                    };
                }

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
    }, [props]);
}

const lengthPropType = PropTypes.shape({
    extraSmall: PropTypes.number,
    small: PropTypes.number,
    medium: PropTypes.number,
    large: PropTypes.number,
    extraLarge: PropTypes.number,
});

DynamicGroup.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    type: PropTypes.oneOf([
        // Supported Group Types
        'COL',
        'FRAGMENT',
        'GROUP',
        'ROW',
    ]).isRequired,
    length: lengthPropType,
    offset: lengthPropType,
};

DynamicGroup.defaultProps = {
    length: undefined,
    offset: undefined,
};

export default DynamicGroup;
