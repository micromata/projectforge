import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import moment from 'moment';
import { Button } from '../../../../../design';
import { DynamicLayoutContext } from '../../../context';

function CustomizedBookLendOutComponent({ user, jsTimestampFormatMinutes }) {
    const { data, ui, callAction } = React.useContext(DynamicLayoutContext);

    const lendOut = () => callAction({
        responseAction: {
            url: 'book/lendOut',
            targetType: 'POST',
        },
    });
    const handBack = () => callAction({
        responseAction: {
            url: 'book/returnBook',
            targetType: 'POST',
        },
    });

    return React.useMemo(
        () => (
            <>
                {data.lendOutBy && data.lendOutDate
                    ? (
                        <>
                            <span className="mr-4">
                                {`${data.lendOutBy.displayName}, ${moment(data.lendOutDate).format(jsTimestampFormatMinutes)}`}
                            </span>
                            {user.username === data.lendOutBy.username
                                ? (
                                    <Button color="danger" onClick={handBack}>
                                        {ui.translations['book.returnBook']}
                                    </Button>
                                )
                                : undefined}
                        </>
                    )
                    : undefined}
                <Button color="link" onClick={lendOut}>
                    {ui.translations['book.lendOut']}
                </Button>
            </>
        ),
        [data.lendOutBy, data.lendOutDate],
    );
}

CustomizedBookLendOutComponent.propTypes = {
    user: PropTypes.shape({}).isRequired,
};

CustomizedBookLendOutComponent.defaultProps = {
};

const mapStateToProps = ({ authentication }) => ({
    user: authentication.user,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(CustomizedBookLendOutComponent);
