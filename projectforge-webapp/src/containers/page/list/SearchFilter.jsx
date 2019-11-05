import React from 'react';
import CreatableSelect from 'react-select/creatable';
import DynamicActionGroup from '../../../components/base/dynamicLayout/action/DynamicActionGroup';
import { DynamicLayoutContext } from '../../../components/base/dynamicLayout/context';
import { Card, CardBody, Col, FormGroup, Label, Row } from '../../../components/design';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import ReactSelect from '../../../components/design/ReactSelect';
import { getNamedContainer } from '../../../utilities/layout';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import FavoritesPanel from '../../panel/favorite/FavoritesPanel';
import { ListPageContext } from './ListPageContext';


function SearchFilter() {
    const {
        ui,
        renderLayout,
        setData,
    } = React.useContext(DynamicLayoutContext);
    const {
        category,
        filter,
        filterFavorites,
        filterHelper,
        setFilterFavorites,
        setUI,
    } = React.useContext(ListPageContext);

    const saveUpdateResponse = (
        {
            data: responseData,
            ui: responseUI,
            filter: responseFilter,
            filterFavorites: responseFilterFavorites,
        },
    ) => {
        if (responseData) {
            setData(responseData);
        }
        if (responseUI) {
            setUI(responseUI);
        }
        if (responseFilter) {
            filterHelper.setFilterState(responseFilter);
        }
        if (responseFilterFavorites) {
            setFilterFavorites(responseFilterFavorites);
        }
    };

    const fetchFavorites = (action, { params = {}, body }) => fetch(
        getServiceURL(`${category}/filter/${action}`, params),
        {
            method: body ? 'POST' : 'GET',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify(body),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(saveUpdateResponse)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteCreate = (newFilterName) => {
        filter.name = newFilterName;
        fetchFavorites('create', { body: filter });
    };
    const handleFavoriteDelete = id => fetchFavorites('delete', { params: { id } });
    const handleFavoriteSelect = id => fetchFavorites('select', { params: { id } });
    const handleFavoriteRename = (id, newName) => fetchFavorites('rename', {
        params: {
            id,
            newName,
        },
    });
    const handleFavoriteUpdate = () => fetchFavorites('update', { body: filter });

    const handleMaxRowsChange = ({ value }) => filterHelper.setFilter('maxRows', value);

    const handleSearchFilterValueChange = filterHelper.editEntry;

    const handleSearchFilterChange = (value, meta) => {
        switch (meta.action) {
            case 'clear':
                filterHelper.clearEntries();
                break;
            case 'create-option':
                filterHelper.addEntry({ value: { value: value[value.length - 1].value } });
                break;
            case 'select-option':
                filterHelper.addEntry({
                    field: meta.option.id,
                    value: '',
                    isNew: true,
                });
                break;
            case 'pop-value':
            case 'remove-value':
                if (meta.removedValue !== undefined) {
                    filterHelper.removeEntry(meta.removedValue.id || meta.removedValue.label);
                }
                break;
            default:
        }
    };

    const select = React.useMemo(() => {
        const searchFilter = getNamedContainer('searchFilter', ui.namedContainers);

        if (!searchFilter) {
            return <React.Fragment />;
        }

        const options = searchFilter.content.map(option => ({
            ...option,
            value: option.key,
            label: option.label,
        }));
        const entries = filter.entries || [];

        return (
            <CreatableSelect
                components={{
                    MultiValueLabel: EditableMultiValueLabel,
                }}
                isClearable
                isMulti
                name="searchFilter"
                options={options}
                getOptionValue={option => option.key}
                onChange={handleSearchFilterChange}
                placeholder={ui.translations['select.placeholder']}
                setMultiValue={handleSearchFilterValueChange}
                value={entries.map(entry => ({
                    ...entry,
                    key: entry.field || entry.value.value,
                    label: entry.field || entry.value.value,
                    ...Array.findByField(options, 'id', entry.field),
                }))}
                values={entries.reduce((accumulator, currentValue) => ({
                    ...accumulator,
                    [currentValue.field || currentValue.value.value]: currentValue.value,
                }), {})}
            />
        );
    }, [ui.namedContainers, ui.translations, filter.entries]);

    return (
        <Card>
            <CardBody>
                <form>
                    <Row>
                        <Col sm={11}>
                            {select}
                        </Col>
                        <Col sm={1} className="d-flex align-items-center">
                            <FavoritesPanel
                                onFavoriteCreate={handleFavoriteCreate}
                                onFavoriteDelete={handleFavoriteDelete}
                                onFavoriteRename={handleFavoriteRename}
                                onFavoriteSelect={handleFavoriteSelect}
                                onFavoriteUpdate={handleFavoriteUpdate}
                                favorites={filterFavorites}
                                currentFavoriteId={filter.id}
                                isModified
                                closeOnSelect={false}
                                translations={ui.translations}
                                htmlId="searchFilterFavoritesPopover"
                            />
                        </Col>
                    </Row>
                    <Row>
                        <Col sm={8}>
                            <FormGroup row>
                                <Label sm={2}>[Optionen]</Label>
                                <Col sm={10}>
                                    {renderLayout((getNamedContainer('filterOptions', ui.namedContainers) || {}).content)}
                                </Col>
                            </FormGroup>
                        </Col>
                        <Col sm={4}>
                            <ReactSelect
                                label="[Seitengröße]"
                                onChange={handleMaxRowsChange}
                                required
                                translations={ui.translations}
                                value={{
                                    value: filter.maxRows,
                                    label: filter.maxRows,
                                }}
                                values={[
                                    {
                                        value: 25,
                                        label: '25',
                                    },
                                    {
                                        value: 50,
                                        label: '50',
                                    },
                                    {
                                        value: 100,
                                        label: '100',
                                    },
                                    {
                                        value: 200,
                                        label: '200',
                                    },
                                    {
                                        value: 500,
                                        label: '500',
                                    },
                                    {
                                        value: 1000,
                                        label: '1000',
                                    },
                                ]}
                            />
                        </Col>
                    </Row>
                    <FormGroup row>
                        <Col>
                            <DynamicActionGroup actions={ui.actions} />
                        </Col>
                    </FormGroup>
                </form>
            </CardBody>
        </Card>
    );
}

SearchFilter.propTypes = {};

SearchFilter.defaultProps = {};

export default SearchFilter;
