import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import { Collapse } from '../../../design';
import style from '../Navigation.module.scss';
import MenuBadge from './MenuBadge';

class Category extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            collapse: window.innerWidth > 735,
            viewportWidth: 0,
        };

        this.handleWindowResize = this.handleWindowResize.bind(this);
        this.toggle = this.toggle.bind(this);
        this.handleLinkClick = this.handleLinkClick.bind(this);
    }

    componentDidMount() {
        this.handleWindowResize();
        window.addEventListener('resize', this.handleWindowResize);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.handleWindowResize);
    }

    handleWindowResize() {
        const { innerWidth: viewportWidth } = window;


        let collapse = true;

        if (viewportWidth < 735) {
            collapse = false;
        }

        this.setState({
            viewportWidth,
            collapse,
        });
    }

    toggle(event) {
        event.preventDefault();

        const { viewportWidth } = this.state;

        if (viewportWidth > 735) {
            return;
        }

        this.setState(state => ({
            collapse: !state.collapse,
        }));
    }

    handleLinkClick() {
        const { closeMenu } = this.props;

        closeMenu();
    }

    render() {
        const {
            category,
            className,
            // destructuring is necessary. Otherwise its in the dom.
            closeMenu,
            ...props
        } = this.props;
        const { collapse } = this.state;

        return (
            <div className={classNames(style.categoryContainer, className)} {...props}>
                <div
                    className={style.categoryTitle}
                    onClick={this.toggle}
                    role="presentation"
                >
                    {category.title}
                    {category.badge && (
                        <MenuBadge elementKey={category.key}>{category.badge.counter}</MenuBadge>
                    )}
                </div>
                <Collapse isOpen={collapse}>
                    <ul className={style.categoryLinks}>
                        {category.subMenu.map(item => (
                            <li
                                className={style.categoryLink}
                                key={`category-link-${item.key}`}
                            >
                                <Link
                                    onClick={this.handleLinkClick}
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
}

Category.propTypes = {
    category: PropTypes.shape({}).isRequired,
    className: PropTypes.string,
    closeMenu: PropTypes.func,
};

Category.defaultProps = {
    className: undefined,
    closeMenu: undefined,
};

export default Category;
