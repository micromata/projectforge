import React from 'react';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';

function CostNumberComponent() {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const handleNummernkreisChange = (event) => {
        // console.log(event.target.value)
        setData({ nummernkreis: event.target.value });
    };

    const handleBereichChange = (event) => {
        // console.log(event.target.value)
        setData({ bereich: event.target.value });
    };

    const handleTeilbereichChange = (event) => {
        // console.log(event.target.value)
        setData({ teilbereich: event.target.value });
    };

    const handleEndzifferChange = (event) => {
        // console.log(event.target.value)
        setData({ endziffer: event.target.value });
    };

    return React.useMemo(
        () => (
            <>
                <input
                    id="nummernkreis"
                    type="number"
                    size="1"
                    maxLength="1"
                    min="0"
                    max="9"
                    value={data.nummernkreis.toString()}
                    onChange={handleNummernkreisChange}
                />
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
                <input
                    id="teilbereich"
                    type="number"
                    size="2"
                    min="0"
                    max="99"
                    value={data.teilbereich.toString()}
                    onChange={handleTeilbereichChange}
                />
                .
                <input
                    id="endziffer"
                    type="number"
                    size="2"
                    min="0"
                    max="99"
                    value={data.endziffer.toString()}
                    onChange={handleEndzifferChange}
                />
            </>
        ),
    );
}

const mapStateToProps = ({ authentication }) => ({
    user: authentication.user,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(CostNumberComponent);
