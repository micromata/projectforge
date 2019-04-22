import React from 'react';
import PropTypes from 'prop-types';
import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import { Button, UncontrolledTooltip } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { connect } from 'react-redux';
import style from '../../../design/input/Input.module.scss';
import { getServiceURL } from '../../../../utilities/rest';
import UncontrolledReactSelect from './UncontrolledReactSelect';

class UserSelect extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectMeIcon: faSmile,
        };

        this.loadOptions = this.loadOptions.bind(this);
        this.selectMe = this.selectMe.bind(this);
        this.selectMeHover = this.selectMeHover.bind(this);
        this.selectMeUnHover = this.selectMeUnHover.bind(this);
    }

    static getOptionLabel(option) {
        if (!option) {
            return '';
        }
        return `${option.fullname} (${option.username})`;
    }

    loadOptions(inputValue, callback) {
        fetch(getServiceURL('user/aco',
            { search: inputValue }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                callback(json);
            })
            .catch(() => this.setState({}));
    }

    selectMe() {
        const {
            id,
            changeDataField,
            userId,
            username,
            fullname,
        } = this.props;
        changeDataField(id, { id: userId, username, fullname }, true);
    }

    selectMeHover() {
        this.setState({ selectMeIcon: faSmileWink });
    }

    selectMeUnHover() {
        this.setState({ selectMeIcon: faSmile });
    }

    render() {
        const {
            data,
            id,
            changeDataField,
            label,
            translations,
            required,
            userId,
        } = this.props;
        const { selectMeIcon } = this.state;
        const value = Object.getByString(data, id);
        const showSelectMe = (!value || value.id !== userId);
        return (
            <div className="form-row">
                <UncontrolledReactSelect
                    label={label}
                    data={data}
                    id={id}
                    values={[]}
                    changeDataField={changeDataField}
                    translations={translations}
                    valueProperty="id"
                    labelProperty="fullname"
                    loadOptions={this.loadOptions}
                    isRequired={required}
                    getOptionLabel={UserSelect.getOptionLabel}
                    className={style.userSelect}
                />
                <div style={{ display: showSelectMe ? 'block' : 'none' }}>
                    <Button
                        id="selectMe"
                        color="link"
                        className="selectPanelIconLinks"
                        onClick={this.selectMe}
                        onMouseEnter={this.selectMeHover}
                        onMouseLeave={this.selectMeUnHover}
                    >
                        <FontAwesomeIcon
                            icon={selectMeIcon}
                            className={style.icon}
                        />
                    </Button>
                    <UncontrolledTooltip placement="right" target="selectMe">
                        {translations['tooltip.selectMe']}
                    </UncontrolledTooltip>
                </div>
            </div>
        );
    }
}

UserSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    required: PropTypes.bool,
    variables: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
    userId: PropTypes.number.isRequired,
    username: PropTypes.string.isRequired,
    fullname: PropTypes.string.isRequired,
};

UserSelect.defaultProps = {
    required: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    userId: authentication.user.userId,
    username: authentication.user.username,
    fullname: authentication.user.fullname,
});

export default connect(mapStateToProps)(UserSelect);
