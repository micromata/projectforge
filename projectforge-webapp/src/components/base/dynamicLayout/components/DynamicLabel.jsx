import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import { Label } from '../../../design';
import TooltipIcon from '../../../design/TooltipIcon';

function DynamicLabel({ label, tooltip, cssClass }) {
    const id = tooltip ? String.idify(label) : undefined;
    const className = cssClass ? `ui-label ${cssClass}` : 'ui-label';
    return (
        <Label className={className} id={id}>
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
    cssClass: PropTypes.string,
};

export default DynamicLabel;
