import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import style from './Footer.module.scss';

function Footer({ version, releaseTimestamp, updateAvailable }) {
    return (
        <div className={classNames(style.footer, 'footer',)}>
            <ul className={classNames(style.list, style.copyHint)}>
                <li>
                    <a
                        href="https://www.projectforge.org"
                        title="ProjectForge Website"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        &copy;2001-2019{' '}
                    </a>
                    <a
                        href="https://www.micromata.com"
                        title="Micromata GmbH"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        Micromata GmbH
                    </a>
                </li>
                <li>
                    <a
                        href="https://www.projectforge.org"
                        title="www.projectforge.org"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        www.projectforge.org
                    </a>
                </li>
            </ul>
            <ul className={classNames(style.list, style.version)}>
                {updateAvailable
                    ? (
                        <li>
                            <a
                                href="https://sourceforge.net/projects/pforge/files/ProjectForge/"
                                title="Download new version"
                                className={style.news_link}
                            >
                                New Version available
                            </a>
                        </li>
                    )
                    : undefined}
                <li>
                    <a
                        href="https://www.projectforge.org/projectforge-news.html"
                        title="News"
                        className={style.news_link}
                    >
                        {`${version}, ${releaseTimestamp}`}
                    </a>
                </li>
            </ul>

        </div>
    );
}

Footer.propTypes = {
    version: PropTypes.string.isRequired,
    releaseTimestamp: PropTypes.string.isRequired,
    updateAvailable: PropTypes.bool,
};

Footer.defaultProps = {
    updateAvailable: false,
};

export default Footer;
