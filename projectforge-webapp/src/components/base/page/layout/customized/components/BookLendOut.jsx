import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import moment from 'moment';
import { lendOutBook, returnBook } from '../../../../../../actions/customized';
import { Button } from '../../../../../design';

function CustomizedBookLendOutComponent(
    {
        data,
        handBack,
        lendOut,
        translations,
        timestampFormatMinutes,
    },
) {
    let information;

    if (data.lendOutBy && data.lendOutDate) {
        information = (
            <React.Fragment>
                <span>
                    {`${data.lendOutBy.fullname}, ${moment(data.lendOutDate).format(timestampFormatMinutes)}`}
                </span>
                <Button color="danger" onClick={handBack}>
                    {translations['book.returnBook']}
                </Button>
            </React.Fragment>
        );
    }

    return (
        <React.Fragment>
            {information}
            <Button color="outline-primary" onClick={lendOut}>
                {translations['book.lendOut']}
            </Button>
        </React.Fragment>
    );
}

CustomizedBookLendOutComponent.propTypes = {
    translations: PropTypes.shape({
        'book.lendOut': PropTypes.string,
        'book.returnBook': PropTypes.string,
    }).isRequired,
    handBack: PropTypes.func.isRequired,
    lendOut: PropTypes.func.isRequired,
    data: PropTypes.shape({}),
    timestampFormatMinutes: PropTypes.string,
};

CustomizedBookLendOutComponent.defaultProps = {
    data: {},
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
};

const mapStateToProps = ({ authentication }) => ({
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

const actions = {
    handBack: returnBook,
    lendOut: lendOutBook,
};

export default connect(mapStateToProps, actions)(CustomizedBookLendOutComponent);
