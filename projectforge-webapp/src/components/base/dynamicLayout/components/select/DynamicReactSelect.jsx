import PropTypes from 'prop-types';
import React from 'react';
import classNames from 'classnames';
import FavoritesPanel from '../../../../../containers/panel/favorite/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import ReactSelect from '../../../../design/react-select/ReactSelect';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from '../input/DynamicValidationManager';

// Separate the default values into a constant for reuse
const DEFAULT_VALUES = {
    labelProperty: 'label',
    valueProperty: 'value',
    multi: false,
    required: false,
};

// Modified to use default parameters matching the previous defaultProps
export const extractDataValue = ({
    data,
    id,
    labelProperty = DEFAULT_VALUES.labelProperty,
    multi = DEFAULT_VALUES.multi,
    valueProperty = DEFAULT_VALUES.valueProperty,
    values,
}) => {
    let dataValue = Object.getByString(data, id);
    if (!multi && dataValue && values?.length > 0) {
        // For react-select it seems to be important, that the current selected element matches
        // its value of the values list.
        const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
        const value = values.find((it) => it[valueProperty] === valueOfArray);

        if (value) {
            dataValue = value;
        }
    }

    if (typeof dataValue === 'string') {
        return {
            [labelProperty || 'displayName']: dataValue,
            [valueProperty || 'id']: dataValue,
        };
    }

    return dataValue;
};

// Function using destructured props with default values
function DynamicReactSelect({
    id,
    label,
    favorites,
    additionalLabel,
    className,
    getOptionLabel,
    labelProperty = DEFAULT_VALUES.labelProperty,
    loadOptions,
    multi = DEFAULT_VALUES.multi,
    required = DEFAULT_VALUES.required,
    valueProperty = DEFAULT_VALUES.valueProperty,
    // Other props
    values,
    autoCompletion,
    ...restProps
}) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const currentValue = extractDataValue({
        data,
        id,
        labelProperty,
        multi,
        valueProperty,
        values,
    });

    const autoCompletionData = {};

    if (autoCompletion?.urlparams) {
        Object.keys(autoCompletion.urlparams).forEach((key) => {
            autoCompletionData[key] = Object.getByString(data, autoCompletion.urlparams[key]);
        });
    }

    return React.useMemo(() => {
        const onChange = (newValue) => {
            if (autoCompletion?.type) {
                setData({ [id]: newValue });
                return;
            }

            setData({ [id]: (newValue || {})[valueProperty] });
        };

        const onFavoriteSelect = (favoriteId, name) => {
            const newValue = {
                [valueProperty]: favoriteId,
                [labelProperty]: name,
            };
            onChange(newValue);
        };

        const loadOptionsHandler = loadOptions || ((search, callback) => {
            if (!autoCompletion?.url) return;

            fetch(
                getServiceURL(
                    autoCompletion.url.replace(':search', encodeURIComponent(search)),
                    autoCompletionData,
                ),
                {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        Accept: 'application/json',
                    },
                },
            )
                .then(handleHTTPErrors)
                .then((response) => response.json())
                .then(callback);
        });

        const url = autoCompletion?.url;

        let favoritesElement;
        if (favorites?.length > 0) {
            favoritesElement = (
                <FavoritesPanel
                    onFavoriteSelect={onFavoriteSelect}
                    favorites={favorites}
                    translations={ui.translations}
                    htmlId={`dynamicFavoritesPopover-${ui.uid}-${id}`}
                />
            );
        }

        return (
            <DynamicValidationManager id={id}>
                <ReactSelect
                    autoCompletion={autoCompletion}
                    className={classNames('invalid', className)}
                    onChange={onChange}
                    translations={ui.translations}
                    id={id}
                    label={label}
                    favorites={favorites}
                    additionalLabel={additionalLabel}
                    getOptionLabel={getOptionLabel}
                    labelProperty={labelProperty}
                    loadOptions={(url?.length > 0) ? loadOptionsHandler : undefined}
                    multi={multi}
                    required={required}
                    valueProperty={valueProperty}
                    values={values}
                    {...restProps}
                    value={currentValue}
                />
                {favoritesElement}
            </DynamicValidationManager>
        );
    }, [data[id], currentValue, setData, values, autoCompletionData]);
}

DynamicReactSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({})),
    favorites: PropTypes.arrayOf({}),
    additionalLabel: PropTypes.string,
    autoCompletion: PropTypes.shape({
        url: PropTypes.string,
        urlparams: PropTypes.shape({}),
    }),
    className: PropTypes.string,
    getOptionLabel: PropTypes.func,
    labelProperty: PropTypes.string,
    loadOptions: PropTypes.func,
    multi: PropTypes.bool,
    required: PropTypes.bool,
    valueProperty: PropTypes.string,
};

export default DynamicReactSelect;
