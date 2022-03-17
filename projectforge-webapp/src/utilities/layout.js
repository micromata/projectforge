export const getNamedContainer = (id, namedContainers) => {
    if (!namedContainers) {
        return undefined;
    }

    return namedContainers.find((current) => current.id === id);
};

export const getTranslation = (key, translations) => {
    if (!translations) {
        return '';
    }

    return translations[key];
};

export const formatTimeUnit = (time) => `${time !== undefined && Number(time) < 10 ? '0' : ''}${time}`;
