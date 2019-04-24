import React from 'react';
import PropTypes from 'prop-types';
import { faSmile, faSmileWink } from '@fortawesome/free-regular-svg-icons';
import { Button, UncontrolledTooltip } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { connect } from 'react-redux';
import style from '../../../design/input/Input.module.scss';
import { getServiceURL } from '../../../../utilities/rest';
import UncontrolledReactSelect from './UncontrolledReactSelect';
import ReactSelect from './ReactSelect';

class UserSelect extends React.Component {
    constructor(props) {
        super(props);
        const dataValue = UncontrolledReactSelect.extractDataValue(props);
        this.state = {
            value: dataValue,
            selectMeIcon: faSmile,
        };

        this.loadOptions = this.loadOptions.bind(this);
        this.onChange = this.onChange.bind(this);
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

    onChange(newValue) {
        this.setState({ value: newValue });
        const { id, changeDataField } = this.props;
        changeDataField(id, newValue);
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
        const me = {
            id: userId,
            username,
            fullname,
        };
        this.setState({ value: me });
        changeDataField(id, me);
    }

    selectMeHover() {
        this.setState({ selectMeIcon: faSmileWink });
    }

    selectMeUnHover() {
        this.setState({ selectMeIcon: faSmile });
    }

    render() {
        const { value } = this.state;
        const {
            required,
            userId,
            translations,
            ...props
        } = this.props;
        const { selectMeIcon } = this.state;
        const showSelectMe = (!value || value.id !== userId);

        return (
            <div className="form-row">
                <ReactSelect
                    value={value}
                    onChange={this.onChange}
                    {...props}
                    values={[]}
                    valueProperty="id"
                    labelProperty="fullname"
                    loadOptions={this.loadOptions}
                    isRequired={required}
                    translations={translations}
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
