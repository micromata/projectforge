import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import { Label } from '../../../design';
import TooltipIcon from '../../../design/TooltipIcon';

function DynamicLabel({ label, tooltip }) {
    const id = tooltip ? String.idify(label) : undefined;
    return (
        <Label id={id}>
            {label}
            {tooltip && (
                <>
                    <TooltipIcon />
                    <UncontrolledTooltip placement="auto" target={id}>
                        {tooltip}
                    </UncontrolledTooltip>
                </>
            )}
        </Label>
    );
}

DynamicLabel.propTypes = {
    label: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
};

DynamicLabel.defaultProps = {
    tooltip: undefined,
};

export default DynamicLabel;
