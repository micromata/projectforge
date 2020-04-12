import PropTypes from 'prop-types';
import React from 'react';
import { DynamicLayoutContext } from '../../context';

function DynamicAttachmentList(
    {
        id,
        value,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    console.log(data, props);
    return React.useMemo(() => {
        return (
            <React.Fragment>
                Attachments
            </React.Fragment>
        );
    }, [setData]);
}

DynamicAttachmentList.propTypes = {
    id: PropTypes.string.isRequired,
    readOnly: PropTypes.boolean,
};

DynamicAttachmentList.defaultProps = {
    readOnly: false,
};

export default DynamicAttachmentList;
