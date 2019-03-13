import PropTypes from 'prop-types';

export const categoryItemPropType = PropTypes.shape({
    name: PropTypes.string,
    url: PropTypes.string,
});

export const categoryPropType = PropTypes.shape({
    name: PropTypes.string,
    items: PropTypes.arrayOf(categoryItemPropType),
});

export const buttonPropType = PropTypes.shape({
    style: PropTypes.oneOf(['primary', 'secondary', 'success', 'info', 'warning', 'danger', 'link']),
    title: PropTypes.string,
    action: PropTypes.oneOf(['cancel', 'markAsDeleted', 'save', 'custom']),
    customEndpoint: PropTypes.string,
});
