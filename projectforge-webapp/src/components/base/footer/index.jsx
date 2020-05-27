import classNames from 'classnames';
import React from 'react';
import { SystemStatusContext } from '../../../containers/SystemStatusContext';
import style from './Footer.module.scss';

function Footer() {
    const {
        version,
        buildTimestamp,
        scmId,
        copyRightYears,
    } = React.useContext(SystemStatusContext);

    return (
        <div className={style.footer}>
            <ul className={classNames(style.list, style.copyHint)}>
                <li>
                    <a
                        href="https://www.projectforge.org"
                        title="ProjectForge Website"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        &copy;
                        {`${copyRightYears}`}
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
                <li>
                    <a
                        href="https://www.projectforge.org/projectforge-news.html"
                        title="News"
                        className={style.news_link}
                    >
                        {`${scmId}, ${version}, ${buildTimestamp}`}
                    </a>
                </li>
            </ul>

        </div>
    );
}

Footer.propTypes = {};

Footer.defaultProps = {};

export default Footer;
