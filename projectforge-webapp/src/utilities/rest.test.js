import { baseURL, createQueryParams, getServiceURL } from './rest';

it('base url', () => {
    expect(baseURL)
        .toBe('/rs');
});

describe('create query params', () => {
    it('empty params', () => {
        const params = {};
        const expectedParams = '';

        expect(createQueryParams(params))
            .toBe(expectedParams);
    });

    it('one param', () => {
        const params = {
            amount: 10,
        };
        const expectedParams = 'amount=10';

        expect(createQueryParams(params))
            .toBe(expectedParams);
    });

    it('several params', () => {
        const params = {
            amount: 10,
            flavor: 'sweet',
        };
        const expectedParams = 'amount=10&flavor=sweet';

        expect(createQueryParams(params))
            .toBe(expectedParams);
    });

    it('several params with illegal URI chars', () => {
        const params = {
            amount: 10,
            flavor: 'sweet',
            name: 'Schwarzwälder Kirschtorte',
        };
        const expectedParams = 'amount=10&flavor=sweet&name=Schwarzw%C3%A4lder%20Kirschtorte';

        expect(createQueryParams(params))
            .toBe(expectedParams);
    });
});

describe('get service url', () => {
    it('undefined params', () => {
        const serviceURL = 'cakes/order';
        const expectedServiceURL = '/rs/cakes/order';

        expect(getServiceURL(serviceURL))
            .toBe(expectedServiceURL);
    });

    it('empty params', () => {
        const params = {};
        const serviceURL = 'cakes/order';
        const expectedServiceURL = '/rs/cakes/order';

        expect(getServiceURL(serviceURL, params))
            .toBe(expectedServiceURL);
    });

    it('with params', () => {
        const params = {
            id: 1,
            amount: 123,
        };
        const serviceURL = 'cakes/order';
        const expectedServiceURL = '/rs/cakes/order?id=1&amount=123';

        expect(getServiceURL(serviceURL, params))
            .toBe(expectedServiceURL);
    });
});
