import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import { Collapse } from '../../../design';
import style from '../Navigation.module.scss';

class Category extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            collapse: window.innerWidth > 735,
            viewportWidth: 0,
        };

        this.handleWindowResize = this.handleWindowResize.bind(this);
        this.toggle = this.toggle.bind(this);
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

    render() {
        const { category, className, ...props } = this.props;
        const { collapse } = this.state;

        return (
            <div className={classNames(style.categoryContainer, className)} {...props}>
                <div
                    className={style.categoryTitle}
                    onClick={this.toggle}
                    role="presentation"
                >
                    {category.title}
                </div>
                <Collapse isOpen={collapse}>
                    <ul className={style.categoryLinks}>
                        {category.subMenu.map(item => (
                            <li
                                className={style.categoryLink}
                                key={`category-link-${item.key}`}
                            >
                                <Link to={`/${item.url}/`}>{item.title}</Link>
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
};

Category.defaultProps = {
    className: undefined,
};

export default Category;
