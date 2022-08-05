import PropTypes from 'prop-types';
import React from 'react';
import TextAutoCompletion from '../../../../design/input/autoCompletion/TextAutoCompletion';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';
import { evalServiceURL } from '../../../../../utilities/rest';

function DynamicAutoCompletion(
    {
        id,
        url,
        urlparams,
        label,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const autoCompletionData = {};

    let nUrl = url;

    if (urlparams) {
        Object.keys(urlparams).forEach((key) => {
            autoCompletionData[key] = Object.getByString(data, urlparams[key]);
        });
        nUrl = evalServiceURL(url, autoCompletionData);
    }

    return React.useMemo(() => (
        <DynamicValidationManager id={id}>
            <TextAutoCompletion
                inputId={`${ui.uid}-${id}`}
                inputProps={{ label }}
                onChange={(completion) => setData({ [id]: completion })}
                url={nUrl}
                value={data[id]}
                {...props}
            />
        </DynamicValidationManager>
    ), [data[id], id, url, label, props]);
}

DynamicAutoCompletion.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    urlparams: PropTypes.shape({}),
};

DynamicAutoCompletion.defaultProps = {};

export default DynamicAutoCompletion;
