import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router';
import { Collapse } from 'reactstrap';
import style from '../Navigation.module.scss';
import MenuBadge from './MenuBadge';

function Category({
    category, className, closeMenu, ...props
}) {
    const [collapse, setCollapse] = useState(window.innerWidth > 735);
    const [viewportWidth, setViewportWidth] = useState(0);

    const handleWindowResize = () => {
        const { innerWidth: width } = window;
        setViewportWidth(width);

        if (width < 735) {
            setCollapse(false);
        } else {
            setCollapse(true);
        }
    };

    const handleLinkClick = () => {
        closeMenu?.();
    };

    const toggle = (event) => {
        event.preventDefault();

        if (viewportWidth > 735) {
            return;
        }

        setCollapse((prevCollapse) => !prevCollapse);
    };

    useEffect(() => {
        handleWindowResize();
        window.addEventListener('resize', handleWindowResize);

        return () => {
            window.removeEventListener('resize', handleWindowResize);
        };
    }, []);

    return (
        <div className={classNames(style.categoryContainer, className)} {...props}>
            <button
                type="button"
                className={style.categoryTitle}
                onClick={toggle}
            >
                {category.title}
                {category.badge && (
                    <MenuBadge elementKey={category.key}>{category.badge.counter}</MenuBadge>
                )}
            </button>
            <Collapse isOpen={collapse}>
                <ul className={style.categoryLinks}>
                    {category.subMenu.map((item) => (
                        <li
                            className={style.categoryLink}
                            key={`category-link-${item.key}`}
                        >
                            <Link
                                onClick={handleLinkClick}
                                to={`/${item.url}/`}
                            >
                                {item.title}
                                {item.badge && (
                                    <MenuBadge
                                        elementKey={item.key}
                                        tooltip={item.badge.tooltip}
                                    >
                                        {item.badge.counter}
                                    </MenuBadge>
                                )}
                            </Link>
                        </li>
                    ))}
                </ul>
            </Collapse>
        </div>
    );
}

Category.propTypes = {
    category: PropTypes.shape({
        title: PropTypes.string,
        badge: PropTypes.shape({
            counter: PropTypes.number,
            style: PropTypes.string,
        }),
        key: PropTypes.string,
        subMenu: PropTypes.arrayOf(PropTypes.shape({
            map: PropTypes.shape({}),
        })),
    }).isRequired,
    className: PropTypes.string,
    closeMenu: PropTypes.func,
};

export default Category;
