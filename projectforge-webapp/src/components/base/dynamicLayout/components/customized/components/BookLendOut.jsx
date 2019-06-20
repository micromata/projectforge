import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { lendOutBook, returnBook } from '../../../../../../actions/customized';
import { Button } from '../../../../../design';
import { DynamicLayoutContext } from '../../../context';

function CustomizedBookLendOutComponent(
    {
        handBack,
        lendOut,
        user,
    },
) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => (
            <React.Fragment>
                {data.lendOutBy && data.lendOutDate
                    ? (
                        <React.Fragment>
                            <span className="mr-4">
                                {`${data.lendOutBy.fullname}, ${data.lendOutDate}`}
                            </span>
                            {user.username === data.lendOutBy.username
                                ? (
                                    <Button color="danger" onClick={handBack}>
                                        {ui.translations['book.returnBook']}
                                    </Button>
                                )
                                : undefined}
                        </React.Fragment>
                    )
                    : undefined}
                <Button color="link" onClick={lendOut}>
                    {ui.translations['book.lendOut']}
                </Button>
            </React.Fragment>
        ),
        [data.lendOutBy, data.lendOutDate],
    );
}

CustomizedBookLendOutComponent.propTypes = {
    handBack: PropTypes.func.isRequired,
    lendOut: PropTypes.func.isRequired,
    user: PropTypes.shape({}).isRequired,
};

CustomizedBookLendOutComponent.defaultProps = {
};

const mapStateToProps = state => ({
    user: state.authentication.user,
});

const actions = {
    handBack: returnBook,
    lendOut: lendOutBook,
};

export default connect(mapStateToProps, actions)(CustomizedBookLendOutComponent);
