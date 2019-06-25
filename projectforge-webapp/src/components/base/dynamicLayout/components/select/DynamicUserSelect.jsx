import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { Button, UncontrolledTooltip } from '../../../../design';
import style from '../../../../design/input/Input.module.scss';
import ReactSelect from '../../../../design/ReactSelect';
import { DynamicLayoutContext } from '../../context';
import { extractDataValue } from './DynamicReactSelect';

const getOptionLabel = (option) => {
    if (option) {
        return `${option.fullname} (${option.username})`;
    }

    return '';
};

function DynamicUserSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const [value, setValue] = React.useState(extractDataValue({ data, ...props }));
    const [selectMeIcon, setSelectMeIcon] = React.useState(faSmile);

    const {
        fullname,
        id,
        required,
        userId,
        username,
    } = props;

    return React.useMemo(() => {
        const handleChange = (newValue) => {
            setValue(newValue);
            setData({
                [id]: newValue,
            });
        };

        const selectMe = () => handleChange({
            id: userId,
            username,
            fullname,
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
                    translations={ui.translations}
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
                        {ui.translations['tooltip.selectMe']}
                    </UncontrolledTooltip>
                </div>
            </div>
        );
    }, [props, value, selectMeIcon]);
}

DynamicUserSelect.propTypes = {
    fullname: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    userId: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    required: PropTypes.bool,
};

DynamicUserSelect.defaultProps = {
    required: false,
};

const mapStateToProps = ({ authentication }) => ({
    userId: authentication.user.userId,
    username: authentication.user.username,
    fullname: authentication.user.fullname,
});

export default connect(mapStateToProps)(DynamicUserSelect);
