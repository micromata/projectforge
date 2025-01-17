import React, { useEffect, useState } from 'react';
import { Card, CardBody, CardHeader, CardText, CardTitle, Container } from 'reactstrap';
import useActions from '../../actions/useActions';
import { getServiceURL } from '../../utilities/rest';
import { loadUserStatus } from '../../actions';

function IndexPage() {
    const [translations, setTranslations] = useState(null);
    const [checkAuthentication] = useActions([loadUserStatus]);

    useEffect(() => {
        const fetchContent = async () => {
            try {
                const response = await fetch(getServiceURL('index'), {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        Accept: 'application/json',
                    },
                });

                if (!response.ok) {
                    throw Error('Failed to fetch index content');
                }

                const content = await response.json();
                setTranslations(content.translations);
            } catch {
                checkAuthentication();
            }
        };

        fetchContent();
    }, []);

    if (!translations) {
        return <div>Loading...</div>;
    }

    return (
        <Container>
            <Card>
                <CardHeader>
                    <CardTitle>
                        {translations['index.welcome']}
                    </CardTitle>
                </CardHeader>
                <CardBody>
                    <CardText>
                        {translations['index.website']}
                        {': '}
                        <a href="https://www.projectforge.org" target="_blank" rel="noreferrer">www.projectforge.org</a>
                    </CardText>
                    <CardText>
                        {translations['index.development']}
                        {': '}
                        <a href="https://github.com/micromata/projectforge/" target="_blank" rel="noreferrer">github.com/micromata/projectforge/</a>
                    </CardText>
                </CardBody>
            </Card>
        </Container>
    );
}

export default IndexPage;
