import PropTypes from 'prop-types';
import React from 'react';
import FavoritesPanel from '../../../../../containers/panel/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import ReactSelect from '../../../../design/ReactSelect';
import { DynamicLayoutContext } from '../../context';

export const extractDataValue = (
    {
        data,
        id,
        isMulti,
        valueProperty,
        values,
    },
) => {
    let dataValue = Object.getByString(data, id);
    if (!isMulti && dataValue && values && values.length && values.length > 0) {
        // For react-select it seems to be important, that the current selected element matches
        // its value of the values list.
        const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
        dataValue = values.find(it => it[valueProperty] === valueOfArray);
    }
    return dataValue;
};

function DynamicReactSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const { id, favorites, autoCompletion } = props;
    const [value, setValue] = React.useState(extractDataValue({ data, ...props }));

    return React.useMemo(() => {
        const onChange = (newValue) => {
            setValue(newValue);
            setData({ [id]: newValue });
        };

        const loadOptions = (search, callback) => fetch(
            getServiceURL(autoCompletion.url, { search }),
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
            .then(callback);

        const url = autoCompletion ? autoCompletion.url : undefined;

        let favoritesElement;
        if (favorites) {
            favoritesElement = (
                <FavoritesPanel
                    id="taskSelectFavorites"
                    onFavoriteSelect={onChange}
                    favorites={favorites}
                    translations={ui.translations}
                />
            );
        }

        return (
            <React.Fragment>
                <ReactSelect
                    onChange={onChange}
                    translations={ui.translations}
                    {...props}
                    value={value}
                    loadOptions={(url && url.length > 0) ? loadOptions : undefined}
                />
                {favoritesElement}
            </React.Fragment>
        );
    }, [data[id], value]);
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
