import PropTypes from 'prop-types';
import React from 'react';
import { Button, UncontrolledCollapse } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col, FormGroup, Row } from '../../../design';
import { DynamicLayoutContext } from '../context';
import style from '../../../design/list/List.module.scss';

export const buildLengthForColumn = (length, offset = undefined) => (offset
    ? Object.keys(length)
        .reduce((previousValue, key) => ({
            ...previousValue,
            [key]: {
                size: length[key],
                offset: offset[key],
            },
        }), {})
    : length);

// A Component to put a tag around dynamic layout content
function DynamicGroup(props) {
    const {
        content,
        length,
        offset,
        type,
        collapseTitle,
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
                        ...(buildLengthForColumn(length, offset)),
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

        if (collapseTitle) {
            const id = String.idify(collapseTitle);
            return (
                <Tag {...groupProperties}>
                    <Button id={id} color="link">
                        {collapseTitle}
                        <FontAwesomeIcon icon={faChevronDown} className={style.chevron} />
                    </Button>
                    <UncontrolledCollapse toggler={`#${id}`}>
                        {renderLayout(content)}
                    </UncontrolledCollapse>
                </Tag>
            );
        }
        // Render tag and further content
        return (
            <Tag {...groupProperties}>
                {renderLayout(content)}
            </Tag>
        );
    }, [props]);
}

export const lengthPropType = PropTypes.shape({
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
    collapseTitle: PropTypes.string,
};

export default DynamicGroup;
