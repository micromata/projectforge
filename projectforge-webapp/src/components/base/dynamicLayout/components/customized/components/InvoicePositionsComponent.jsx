import React from 'react';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';

function InvoicePositionsComponent({user}) {
    const { data, callAction} = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => (
            <React.Fragment>
                <div>
                    <label>div</label>
                </div>
            </React.Fragment>
        )
    );
}

const mapStateToProps = ({ authentication }) => ({
    user: authentication.user,
});


export default connect(mapStateToProps)(InvoicePositionsComponent);
