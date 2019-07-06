import React from 'react';
import CreatableSelect from 'react-select/lib/Creatable';
import { DynamicLayoutContext } from '../../../components/base/dynamicLayout/context';
import ActionGroup from '../../../components/base/page/action/Group';
import { Card, CardBody, Col, FormGroup, Label, Row } from '../../../components/design';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import ReactSelect from '../../../components/design/ReactSelect';
import { getNamedContainer } from '../../../utilities/layout';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import FavoritesPanel from '../../panel/FavoritesPanel';


function SearchFilter() {
    const {
        filter,
        filterHelper,
        ui,
        renderLayout,
        setData,
        setUI,
        category,
    } = React.useContext(DynamicLayoutContext);

    const saveUpdateResponse = ({ data: responseData, ui: responseUI, filter: responseFilter }) => {
        setData(responseData);
        setUI(responseUI);
        filterHelper.setFilter(responseFilter);
    };

    const handleFavoriteCreate = (newFilterName) => {
        filter.name = newFilterName;
        fetch(getServiceURL(`${category}/filter/create`), {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify({ filter }),
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(saveUpdateResponse)
            .catch(error => alert(`Internal error: ${error}`));
    };
    const handleFavoriteDelete = (id) => {
        fetch(getServiceURL(`${category}/filter/delete`,
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(saveUpdateResponse)
            .catch(error => alert(`Internal error: ${error}`));
    };
    const handleFavoriteSelect = (id) => {
        fetch(getServiceURL(`${category}/filter/select`,
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(saveUpdateResponse)
            .catch(error => alert(`Internal error: ${error}`));
    };
    const handleFavoriteRename = (id, newName) => console.log(id, newName);

    const handleFavoriteUpdate = (id) => {
        fetch(getServiceURL(`${category}/filter/update`,
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(saveUpdateResponse)
            .catch(error => alert(`Internal error: ${error}`));
    };

    const handleMaxRowsChange = ({ value }) => filterHelper.setSearchFilter('maxRows', value);

    const handleSearchFilterValueChange = filterHelper.editEntry;

    const handleSearchFilterChange = (value, meta) => {
        switch (meta.action) {
            case 'clear':
                filterHelper.clearEntries();
                break;
            case 'create-option':
                filterHelper.addEntry({ search: value[value.length - 1].value });
                break;
            case 'select-option':
                filterHelper.addEntry({
                    field: meta.option.id,
                    value: '',
                });
                break;
            case 'pop-value':
            case 'remove-value':
                filterHelper.removeEntry(meta.removedValue.id || meta.removedValue.label);
                break;
            default:
        }
    };

    const searchFilter = getNamedContainer('searchFilter', ui.namedContainers);

    return (
        <Card>
            <CardBody>
                <form>
                    <Row>
                        <Col sm={11}>
                            <CreatableSelect
                                components={{
                                    MultiValueLabel: EditableMultiValueLabel,
                                }}
                                isClearable
                                isMulti
                                name="searchFilter"
                                options={searchFilter ? searchFilter.content.map(option => ({
                                    ...option,
                                    value: option.key,
                                    // TODO DISPLAYED LABEL CAN BE SET HERE
                                    label: option.id,
                                })) : []}
                                onChange={handleSearchFilterChange}
                                setMultiValue={handleSearchFilterValueChange}
                                values={filter.entries.reduce((accumulator, currentValue) => ({
                                    ...accumulator,
                                    [currentValue.field]: currentValue.value,
                                }), {})}
                            />
                        </Col>
                        <Col sm={1} className="d-flex align-items-center">
                            <FavoritesPanel
                                onFavoriteCreate={handleFavoriteCreate}
                                onFavoriteDelete={handleFavoriteDelete}
                                onFavoriteRename={handleFavoriteRename}
                                onFavoriteSelect={handleFavoriteSelect}
                                onFavoriteUpdate={handleFavoriteUpdate}
                                translations={ui.translations}
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
                                    value: filter.searchFilter.maxRows,
                                    label: filter.searchFilter.maxRows,
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
                            <ActionGroup actions={ui.actions} />
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
