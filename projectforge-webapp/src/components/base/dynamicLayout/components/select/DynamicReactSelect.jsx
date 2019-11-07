import PropTypes from 'prop-types';
import React from 'react';
import FavoritesPanel from '../../../../../containers/panel/favorite/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { resolveJSON } from '../../../../design/input/AutoCompletion';
import ReactSelect from '../../../../design/ReactSelect';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from '../input/DynamicValidationManager';

export const extractDataValue = (
    {
        data,
        id,
        multi,
        valueProperty,
        values,
    },
) => {
    let dataValue = Object.getByString(data, id);
    if (!multi && dataValue && values && values.length && values.length > 0) {
        // For react-select it seems to be important, that the current selected element matches
        // its value of the values list.
        const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
        dataValue = values.find(it => it[valueProperty] === valueOfArray);
    }

    if (typeof dataValue === 'string') {
        return {
            label: dataValue,
            value: dataValue,
        };
    }

    return dataValue;
};

function DynamicReactSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const {
        id,
        favorites,
        autoCompletion,
        labelProperty,
        valueProperty,
        values,
    } = props;

    const value = extractDataValue({ data, ...props });

    return React.useMemo(() => {
        const onChange = (newValue) => {
            if (autoCompletion && autoCompletion.type) {
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

        const loadOptions = (search, callback) => fetch(
            getServiceURL(`${autoCompletion.url}${search}`),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(resolveJSON(callback, autoCompletion.type));

        const url = autoCompletion ? autoCompletion.url : undefined;

        let favoritesElement;
        if (favorites && favorites.length > 0) {
            favoritesElement = (
                <FavoritesPanel
                    onFavoriteSelect={onFavoriteSelect}
                    favorites={favorites}
                    translations={ui.translations}
                    htmlId={`dynamicFavoritesPopover-${id}`}
                />
            );
        }

        return (
            <React.Fragment>
                <DynamicValidationManager id={id}>
                    <ReactSelect
                        className="invalid"
                        onChange={onChange}
                        translations={ui.translations}
                        {...props}
                        value={value}
                        loadOptions={(url && url.length > 0) ? loadOptions : undefined}
                    />
                    {favoritesElement}
                </DynamicValidationManager>
            </React.Fragment>
        );
    }, [data[id], value, setData, values]);
}

DynamicReactSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    values: PropTypes.arrayOf(PropTypes.object),
    favorites: PropTypes.arrayOf(PropTypes.object),
    additionalLabel: PropTypes.string,
    autoCompletion: PropTypes.shape({
        url: PropTypes.string,
    }),
    className: PropTypes.string,
    getOptionLabel: PropTypes.func,
    labelProperty: PropTypes.string,
    loadOptions: PropTypes.func,
    multi: PropTypes.bool,
    required: PropTypes.bool,
    valueProperty: PropTypes.string,
};

DynamicReactSelect.defaultProps = {
    value: undefined,
    favorites: undefined,
    additionalLabel: undefined,
    className: undefined,
    getOptionLabel: undefined,
    labelProperty: 'label',
    loadOptions: undefined,
    multi: false,
    required: false,
    valueProperty: 'value',
};

export default DynamicReactSelect;
