import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { contentPropType } from '../../../../utilities/propTypes';
import { Col, Collapse } from '../../../design';
import { DynamicLayoutContext } from '../context';
import { buildLengthForColumn, lengthPropType } from './DynamicGroup';
import style from './DynamicFieldset.module.scss';

// The Fieldset component enclosed in a col. Very similar to DynamicGroup.
function DynamicFieldset(props) {
    const {
        content,
        title,
        length,
        offset,
        collapsed,
    } = props;

    // Get renderLayout function from context
    const { renderLayout } = React.useContext(DynamicLayoutContext);

    // State for collapsible fieldsets (only used when collapsed prop is boolean)
    const [isOpen, setIsOpen] = React.useState(
        collapsed === false || collapsed === undefined,
    );

    // If collapsed prop is not provided (null/undefined), render static fieldset
    if (collapsed === null || collapsed === undefined) {
        return React.useMemo(
            () => (
                <Col {...buildLengthForColumn(length, offset)}>
                    <fieldset>
                        {title ? <legend>{title}</legend> : undefined}
                        {renderLayout(content)}
                    </fieldset>
                </Col>
            ),
            [content, title, length, offset, renderLayout],
        );
    }

    // Toggle handler
    const handleToggle = () => setIsOpen(!isOpen);

    // Render collapsible fieldset when collapsed prop is boolean
    return (
        <Col {...buildLengthForColumn(length, offset)}>
            <fieldset className={style.collapsibleFieldset}>
                {title ? (
                    <legend>
                        <button
                            type="button"
                            className={style.collapsibleLegend}
                            onClick={handleToggle}
                        >
                            <FontAwesomeIcon
                                icon={faChevronRight}
                                className={classNames(
                                    style.chevronIcon,
                                    { [style.expanded]: isOpen },
                                )}
                            />
                            {title}
                        </button>
                    </legend>
                ) : undefined}
                <Collapse isOpen={isOpen} className={style.collapseContent}>
                    {renderLayout(content)}
                </Collapse>
            </fieldset>
        </Col>
    );
}

DynamicFieldset.propTypes = {
    content: PropTypes.arrayOf(contentPropType).isRequired,
    title: PropTypes.string,
    length: lengthPropType,
    offset: lengthPropType,
    collapsed: PropTypes.bool,
};

export default DynamicFieldset;
