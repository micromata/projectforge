import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { UncontrolledTooltip } from '../../index';
import styles from './AutoCompletion.module.scss';
import ObjectAutoCompletion from './ObjectAutoCompletion';

function ObjectSelect(
    {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        dispatch,
        id,
        label,
        onSelect,
        translations,
        type,
        url,
        user,
        value,
        ...props
    },
) {
    const [selectMeIcon, setSelectMeIcon] = React.useState(faSmile);

    const handleSelectMeHoverBegin = () => setSelectMeIcon(faSmileWink);
    const handleSelectMeHoverEnd = () => setSelectMeIcon(faSmile);
    const handleSelectMeClick = (event) => {
        event.stopPropagation();
        onSelect({
            id: type === 'EMPLOYEE' ? user.employeeId : user.id,
            displayName: user.displayName,
        });

        // Un-hover because the element will be removed from the dom.
        handleSelectMeHoverEnd();
    };

    const hasSelectMe = user
        && ((type === 'USER' && (!value || value.id !== user.id))
            || (type === 'EMPLOYEE' && (!value || value.id !== user.employeeId)));

    let inputProps = {
        label,
    };

    if (hasSelectMe) {
        inputProps = {
            ...inputProps,
            icon: selectMeIcon,
            iconProps: {
                className: styles.selectMeIcon,
                id: `${id}selectMe`,
                onClick: handleSelectMeClick,
                onMouseEnter: handleSelectMeHoverBegin,
                onMouseLeave: handleSelectMeHoverEnd,
            },
        };
    }

    return (
        <>
            <ObjectAutoCompletion
                inputId={id}
                inputProps={inputProps}
                onSelect={onSelect}
                url={url || `${type.toLowerCase()}/autosearch?search=:search`}
                value={value}
                {...props}
            />
            {hasSelectMe && translations['tooltip.selectMe'] && (
                <UncontrolledTooltip placement="top" target={inputProps.iconProps.id}>
                    {translations['tooltip.selectMe']}
                </UncontrolledTooltip>
            )}
        </>
    );
}

ObjectSelect.propTypes = {
    dispatch: PropTypes.func.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    type: PropTypes.oneOf(['USER', 'EMPLOYEE', 'COST1', 'COST2', 'OTHER', 'KONTO']).isRequired,
    translations: PropTypes.shape({
        'tooltip.selectMe': PropTypes.string,
    }),
    url: PropTypes.string,
    user: PropTypes.shape({
        id: PropTypes.number.isRequired,
        displayName: PropTypes.string.isRequired,
        employeeId: PropTypes.number,
    }),
    value: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    }),
};

const mapStateToProps = ({ authentication }) => {
    if (authentication.user) {
        return {
            user: {
                id: authentication.user.userId,
                employeeId: authentication.user.employeeId,
                displayName: authentication.user.username,
            },
        };
    }

    return {};
};

export default connect(mapStateToProps)(ObjectSelect);
