import { faListUl } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { NavItem, NavLink, Nav, Button } from 'reactstrap';
import { Manager, Reference, Popper } from 'react-popper';
import classNames from 'classnames';
import { menuItemPropType } from '../../../../utilities/propTypes';
import { Container } from '../../../design';
import style from '../Navigation.module.scss';
import Category from './Category';
import MenuBadge from './MenuBadge';

function countColumnSize(column) {
    return column
        .reduce((accumulator, currentValue) => accumulator + currentValue.subMenu.length + 1, 0);
}

function CategoriesDropdown({ badge, categories }) {
    const [open, setOpen] = React.useState(false);

    // Creating an array of columns for the categories.
    const columns = [
        [], [], [], [],
    ];

    function useOutsideAlerter(ref) {
        React.useEffect(() => {
            function handleClickOutside(event) {
                if (ref.current && !ref.current.contains(event.target)) {
                    setOpen(false);
                }
            }
            function handleKeyPress(event) {
                if (event.key === 'Escape') {
                    setOpen(false);
                }
            }
            document.addEventListener('mousedown', handleClickOutside);
            document.addEventListener('keydown', handleKeyPress, false);
            return () => {
                document.removeEventListener('mousedown', handleClickOutside);
                document.removeEventListener('keydown', handleClickOutside);
            };
        }, [ref]);
    }

    const wrapperRef = React.useRef(null);
    useOutsideAlerter(wrapperRef);

    // Add the 'Common' category to the first column, so it will always be first.
    columns[0].push(categories[0]);

    // Summarized: Balancing the categories to get less white space.
    categories
    // Remove the first ('common') category.
        .slice(1)
        // Filter out empty categories
        .filter((category) => category.subMenu)
        // Sort the categories by their items size.
        .sort((categoryA, categoryB) => categoryB.subMenu.length - categoryA.subMenu.length)
        // forEach through all categories.
        .forEach((category) => columns
        // Compare all columns and get the smallest one.
            .reduce((columnA, columnB) => (countColumnSize(columnA) < countColumnSize(columnB)
                ? columnA : columnB))
            // Add the category to the smallest column.
            .push(category));

    return (
        <div ref={wrapperRef}>
            <Nav>
                <Manager>
                    <Reference>
                        {({ ref }) => (
                            <NavItem>
                                <NavLink onClick={() => setOpen(!open)} tag={Button} color="link" ref={ref}>
                                    <FontAwesomeIcon icon={faListUl} />
                                    {badge && (
                                        <MenuBadge
                                            elementKey="DROPDOWN_TOGGLE"
                                            isFlying
                                            color={badge.style}
                                        >
                                            {badge.counter}
                                        </MenuBadge>
                                    )}
                                </NavLink>
                            </NavItem>
                        )}
                    </Reference>
                    <Popper placement="bottom-start">
                        {(
                            {
                                ref,
                                style: popperStyle,
                                placement,
                                arrowProps,
                            },
                        ) => (
                            <div
                                ref={ref}
                                style={popperStyle}
                                data-placement={placement}
                                className={classNames(
                                    style.categoryListDropdownMenu,
                                    open && style.open,
                                )}
                            >
                                <Container>
                                    <div className={style.categories}>
                                        {columns.map((column) => (
                                            <div
                                                key={`menu-column-${column.map(({ id }) => id)
                                                    .join('-')}`}
                                                className={style.categoryColumn}
                                            >
                                                {column.map((category) => (
                                                    <Category
                                                        category={category}
                                                        key={`category-${category.id}`}
                                                        closeMenu={() => setOpen(false)}
                                                    />
                                                ))}
                                            </div>
                                        ))}
                                    </div>
                                </Container>
                                <div ref={arrowProps.ref} style={arrowProps.style} />
                            </div>
                        )}
                    </Popper>
                </Manager>
            </Nav>
        </div>
    );
}

CategoriesDropdown.propTypes = {
    categories: PropTypes.arrayOf(menuItemPropType).isRequired,
    badge: PropTypes.shape({
        counter: PropTypes.number,
        style: PropTypes.string,
    }),
};

export default CategoriesDropdown;
