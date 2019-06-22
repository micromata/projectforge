import classNames from 'classnames';
import React from 'react';
import { SystemStatusContext } from '../../../containers/SystemStatusContext';
import style from './Footer.module.scss';

function Footer() {
    const { version, releaseTimestamp, updateAvailable } = React.useContext(SystemStatusContext);

    return (
        <div className={classNames(style.footer, 'footer')}>
            <ul className={classNames(style.list, style.copyHint)}>
                <li>
                    <a
                        href="https://www.projectforge.org"
                        title="ProjectForge Website"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        &copy;2001-2019
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

Footer.propTypes = {};

Footer.defaultProps = {};

export default Footer;
