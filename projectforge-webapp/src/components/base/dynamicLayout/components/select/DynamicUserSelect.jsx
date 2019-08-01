import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import UserSelect from '../../../page/layout/UserSelect';
import { DynamicLayoutContext } from '../../context';
import { extractDataValue } from './DynamicReactSelect';

function DynamicUserSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const [value, setValue] = React.useState(extractDataValue({ data, ...props }));

    const {
        fullname,
        id,
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

        return (
            <UserSelect
                userId={userId}
                fullname={fullname}
                value={value}
                username={username}
                onChange={handleChange}
                translations={ui.translations}
                {...props}
            />
        );
    }, [props, value, setData]);
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
