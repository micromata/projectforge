import PropTypes from 'prop-types';
import React from 'react';
import { Badge } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';

function DynamicBadge(props) {
    const {
        badgeList,
    } = props;

    return (
        <h4>
            {badgeList && badgeList.map((badge) => (
                <Badge color={badge.color} pill>{badge.title}</Badge>
            ))}
        </h4>
    );
}

DynamicBadge.propTypes = {
    badgeList: PropTypes.arrayOf(PropTypes.shape({
        title: PropTypes.string,
        color: colorPropType,
    })),
};

DynamicBadge.defaultProps = {
    badgeList: undefined,
};

export default DynamicBadge;
