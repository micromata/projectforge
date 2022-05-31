import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Button, UncontrolledTooltip } from 'reactstrap';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import style from '../../../design/input/Input.module.scss';
import ReactSelect from '../../../design/react-select/ReactSelect';

const getOptionLabel = (option) => {
    if (option) {
        return `${option.fullname} (${option.username})`;
    }

    return '';
};

function UserSelect(props) {
    const [selectMeIcon, setSelectMeIcon] = React.useState(faSmile);

    const {
        onChange: handleChange,
        required,
        translations,
        user,
        value,
    } = props;

    const selectMe = () => handleChange(user);

    const handleSelectMeHoverBegin = () => setSelectMeIcon(faSmileWink);
    const handleSelectMeHoverEnd = () => setSelectMeIcon(faSmile);

    const loadOptions = (inputValue, callback) => {
        fetch(
            getServiceURL('user/autosearch', { search: inputValue }),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                callback(json);
            });
    };

    return (
        <div className="form-group">
            <ReactSelect
                value={value}
                onChange={handleChange}
                {...props}
                valueProperty="id"
                labelProperty="fullname"
                loadOptions={loadOptions}
                isRequired={required}
                getOptionLabel={getOptionLabel}
                className={style.userSelect}
                translations={translations}
            />
            {(!value || value.id !== user.id) && (
                <>
                    <Button
                        id="selectMe"
                        color="link"
                        className="selectPanelIconLinks"
                        onClick={selectMe}
                        onMouseEnter={handleSelectMeHoverBegin}
                        onMouseLeave={handleSelectMeHoverEnd}
                    >
                        <FontAwesomeIcon
                            icon={selectMeIcon}
                            className={style.icon}
                            size="lg"
                        />
                    </Button>
                    <UncontrolledTooltip placement="right" target="selectMe">
                        {translations['tooltip.selectMe']}
                    </UncontrolledTooltip>
                </>
            )}
        </div>
    );
}

UserSelect.propTypes = {
    onChange: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        'tooltip.selectMe': PropTypes.string.isRequired,
    }).isRequired,
    user: PropTypes.shape({
        fullname: PropTypes.string.isRequired,
        id: PropTypes.number.isRequired,
        username: PropTypes.string.isRequired,
    }).isRequired,
    required: PropTypes.bool,
    value: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]),
};

UserSelect.defaultProps = {
    required: false,
    value: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    user: {
        id: authentication.user.userId,
        username: authentication.user.username,
        fullname: authentication.user.fullname,
    },
});

export default connect(mapStateToProps)(UserSelect);
