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
        urlParams,
        label,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const autoCompletionData = {};

    let nUrl = url;

    if (urlParams) {
        Object.keys(urlParams).forEach((key) => {
            autoCompletionData[key] = Object.getByString(data, urlParams[key]);
        });
        nUrl = evalServiceURL(url, autoCompletionData);
    }

    return React.useMemo(() => (
        <DynamicValidationManager id={id}>
            <TextAutoCompletion
                inputId={`${ui.uid}-${id}`}
                inputProps={{ label }}
                onChange={(completion) => setData({ [id]: completion })}
                url={nUrl} // urlParams (Fin)
                value={data[id]}
                // eslint-disable-next-line react/jsx-props-no-spreading
                {...props}
            />
        </DynamicValidationManager>
    ), [data[id], id, url, label, props]);
}

DynamicAutoCompletion.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    urlParams: PropTypes.shape({}),
};

DynamicAutoCompletion.defaultProps = {};

export default DynamicAutoCompletion;
