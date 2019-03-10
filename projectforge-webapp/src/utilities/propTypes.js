import PropTypes from 'prop-types';

export const categoryItemPropType = PropTypes.shape({
    name: PropTypes.string,
    url: PropTypes.string,
});

export const categoryPropType = PropTypes.shape({
    name: PropTypes.string,
    items: PropTypes.arrayOf(categoryItemPropType),
});
