import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { lendOutBook, returnBook } from '../../../../../../actions/customized';
import { Button } from '../../../../../design';

function CustomizedLendOutComponent(
    {
        data,
        handBack,
        lendOut,
        user,
    },
) {
    let information;

    if (data.lendOutBy && data.lendOutDate) {
        information = (
            <React.Fragment>
                <span>{`${data.lendOutBy.fullname}, ${data.lendOutDate}`}</span>
                {user.username === data.lendOutBy.username
                    ? <Button color="danger" onClick={handBack}>[Zurückgeben]</Button>
                    : undefined}
            </React.Fragment>
        );
    }

    return (
        <React.Fragment>
            {information}
            <Button color="link" onClick={lendOut}>
                [Ausleihen]
            </Button>
        </React.Fragment>
    );
}

CustomizedLendOutComponent.propTypes = {
    handBack: PropTypes.func.isRequired,
    lendOut: PropTypes.func.isRequired,
    user: PropTypes.shape({}).isRequired,
    data: PropTypes.shape({}),
};

CustomizedLendOutComponent.defaultProps = {
    data: {},
};

const mapStateToProps = state => ({
    user: state.authentication.user,
});

const actions = {
    handBack: returnBook,
    lendOut: lendOutBook,
};

export default connect(mapStateToProps, actions)(CustomizedLendOutComponent);
