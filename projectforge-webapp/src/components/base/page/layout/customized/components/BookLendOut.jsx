import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { lendOutBook, returnBook } from '../../../../../../actions/customized';
import { Button } from '../../../../../design';

function CustomizedBookLendOutComponent(
    {
        data,
        handBack,
        lendOut,
        translations,
        user,
    },
) {
    let information;

    if (data.lendOutBy && data.lendOutDate) {
        information = (
            <React.Fragment>
                <span>{`${data.lendOutBy.fullname}, ${data.lendOutDate}`}</span>
                {user.username === data.lendOutBy.username
                    ? (
                        <Button color="danger" onClick={handBack}>
                            {translations['book.returnBook']}
                        </Button>
                    )
                    : undefined}
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
    user: PropTypes.shape({}).isRequired,
    data: PropTypes.shape({}),
};

CustomizedBookLendOutComponent.defaultProps = {
    data: {},
};

const mapStateToProps = state => ({
    user: state.authentication.user,
});

const actions = {
    handBack: returnBook,
    lendOut: lendOutBook,
};

export default connect(mapStateToProps, actions)(CustomizedBookLendOutComponent);
