/* eslint-disable-next-line import/prefer-default-export */
export const getNamedContainer = (id, namedContainers) => {
    if (!namedContainers) {
        return undefined;
    }

    return namedContainers.find(current => current.id === id);
};
