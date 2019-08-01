import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, UncontrolledTooltip } from 'reactstrap';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import style from '../../../design/input/Input.module.scss';
import ReactSelect from '../../../design/ReactSelect';

const getOptionLabel = (option) => {
    if (option) {
        return `${option.fullname} (${option.username})`;
    }

    return '';
};

function UserSelect(props) {
    const [selectMeIcon, setSelectMeIcon] = React.useState(faSmile);

    const {
        fullname,
        onChange: handleChange,
        required,
        translations,
        userId,
        username,
        value,
    } = props;

    const selectMe = () => handleChange({
        username,
        fullname,
        id: userId,
    });

    const handleSelectMeHoverBegin = () => setSelectMeIcon(faSmileWink);
    const handleSelectMeHoverEnd = () => setSelectMeIcon(faSmile);

    const loadOptions = (inputValue, callback) => {
        fetch(
            getServiceURL('user/aco', { search: inputValue }),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then((json) => {
                callback(json);
            });
    };

    return (
        <div className="form-row">
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
            <div style={{ display: (!value || value.id !== userId) ? 'block' : 'none' }}>
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
            </div>
        </div>
    );
}

UserSelect.propTypes = {
    fullname: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        'tooltip.selectMe': PropTypes.string.isRequired,
    }).isRequired,
    userId: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    value: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.arrayOf(PropTypes.shape({})),
    ]).isRequired,
    required: PropTypes.bool,
};

UserSelect.defaultProps = {
    required: false,
};

export default UserSelect;
