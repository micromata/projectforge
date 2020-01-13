import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { UncontrolledTooltip } from '../../index';
import styles from './AutoCompletion.module.scss';
import ObjectAutoCompletion from './ObjectAutoCompletion';


function ObjectSelect(
    {
        id,
        label,
        onSelect,
        translations,
        type,
        user,
        ...props
    },
) {
    const [selectMeIcon, setSelectMeIcon] = React.useState(faSmile);

    const handleSelectMeClick = (event) => {
        event.stopPropagation();
        onSelect({
            id: user.id,
            displayName: user.displayName,
        });
    };
    const handleSelectMeHoverBegin = () => setSelectMeIcon(faSmileWink);
    const handleSelectMeHoverEnd = () => setSelectMeIcon(faSmile);

    const hasSelectMe = type === 'USER' || type === 'EMPLOYEE';

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
        <React.Fragment>
            <ObjectAutoCompletion
                inputId={id}
                inputProps={inputProps}
                onSelect={onSelect}
                url={`${type.toLowerCase()}/autosearch?search=:search`}
                {...props}
            />
            {hasSelectMe && translations['tooltip.selectMe'] && (
                <UncontrolledTooltip placement="top" target={inputProps.iconProps.id}>
                    {translations['tooltip.selectMe']}
                </UncontrolledTooltip>
            )}
        </React.Fragment>
    );
}

ObjectSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    type: PropTypes.oneOf(['USER', 'EMPLOYEE']).isRequired,
    user: PropTypes.shape({
        id: PropTypes.number.isRequired,
        displayName: PropTypes.string.isRequired,
    }).isRequired,
    translations: PropTypes.shape({
        'tooltip.selectMe': PropTypes.string,
    }),
};

ObjectSelect.defaultProps = {
    translations: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    user: {
        id: authentication.user.userId,
        displayName: authentication.user.username,
    },
});

export default connect(mapStateToProps)(ObjectSelect);
