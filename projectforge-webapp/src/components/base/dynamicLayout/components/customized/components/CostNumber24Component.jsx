import React from 'react';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';

function CostNumber24Component() {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const handleBereichChange = (event) => {
        // console.log(event.target.value)
        setData({ bereich: event.target.value });
    };


    return React.useMemo(
        () => (
            <React.Fragment>
                {data.nummernkreis}
                .
                <input
                    id="bereich"
                    type="number"
                    size="3"
                    min="0"
                    max="999"
                    value={data.bereich.toString()}
                    onChange={handleBereichChange}
                />
                .
                ##
                .
                ##
            </React.Fragment>
        ),
    );
}

const mapStateToProps = ({ authentication }) => ({
    user: authentication.user,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});


export default connect(mapStateToProps)(CostNumber24Component);
