import PropTypes from 'prop-types';

export const menuItemPropType = PropTypes.shape({
    title: PropTypes.string,
});

export const buttonPropType = PropTypes.shape({
    style: PropTypes.oneOf(['primary', 'secondary', 'success', 'info', 'warning', 'danger', 'link']),
    title: PropTypes.string,
    id: PropTypes.string,
    handleClick: PropTypes.func,
    type: PropTypes.oneOf(['BUTTON', 'CHECKBOX']),
    checked: PropTypes.bool,
});

export const colorPropType = PropTypes.oneOf([
    'primary',
    'secondary',
    'success',
    'danger',
    'warning',
    'info',
]);

export const selectProps = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    color: colorPropType,
    label: PropTypes.string,
    options: PropTypes.oneOfType([
        PropTypes.arrayOf(PropTypes.shape({
            value: PropTypes.string,
            title: PropTypes.string,
        })),
        PropTypes.arrayOf(PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number,
        ])),
    ]).isRequired,
};

export const dataPropType = PropTypes.shape({});

export const tableColumnsPropType = PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string,
}));

export const badgePropType = PropTypes.shape({
    counter: PropTypes.number,
});

// Supported types for the DynamicLayout
export const dynamicTypePropType = PropTypes.oneOf([
    'COL',
    'FIELDSET',
    'INPUT',
    'LABEL',
    'ROW',
]);

// Content PropType for DynamicLayout
export const contentPropType = PropTypes.shape({
    type: dynamicTypePropType.isRequired,
    key: PropTypes.string.isRequired,
});
