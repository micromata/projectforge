/* eslint-disable */
import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardBody, Button, Alert } from 'reactstrap';
// eslint-disable-next-line import/no-extraneous-dependencies
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faArrowDown, faArrowUp, faMinus, faEllipsisV, faPencilAlt, faSave, faUndo, faTrash, faFolder } from '@fortawesome/free-solid-svg-icons';
import { baseRestURL, handleHTTPErrors } from '../../utilities/rest';
import LoadingContainer from '../design/loading-container';
import styles from './MenuCustomizer.module.scss';

/**
 * Component that allows users to customize their menu via drag and drop.
 * Allows to:
 * - Drag items from main menu to favorites
 * - Drag items around within favorites menu
 * - Create groups and add items to groups
 * - Save the customized menu
 */
function MenuCustomizer() {
    const [loading, setLoading] = useState(true);
    const [menuItems, setMenuItems] = useState({ mainMenu: [], favoritesMenu: [] });
    const [customMenu, setCustomMenu] = useState([]);
    const [newGroupName, setNewGroupName] = useState('');
    const [editingGroup, setEditingGroup] = useState(null);
    const [showGroupInput, setShowGroupInput] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    const loadMenuData = () => {
        setLoading(true);
        // Direct URL call to prevent double URL issue
        fetch(`${baseRestURL}/menu`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                // Keep original menu structure for rendering categories
                const menuStructure = json.mainMenu.menuItems || [];

                // Also create a flattened list for internal operations
                const flattenMenuItems = (items) => {
                    let result = [];
                    if (!items) return result;

                    items.forEach((item) => {
                        result.push(item);
                        if (item.subMenu && item.subMenu.length > 0) {
                            result = result.concat(flattenMenuItems(item.subMenu));
                        }
                    });
                    return result;
                };

                const allMenuItems = flattenMenuItems(menuStructure);

                setMenuItems({
                    // Store both the structured and flattened menu items
                    mainMenu: allMenuItems || [],
                    mainMenuStructured: menuStructure || [],
                    favoritesMenu: json.favoritesMenu.menuItems || [],
                });
                setCustomMenu(json.favoritesMenu.menuItems || []);
                setLoading(false);
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('Error loading menu data:', err);
                setError('Error loading menu data. Please try again.');
                setLoading(false);
            });
    };

    useEffect(() => {
        loadMenuData();
    }, []);

    const handleDragEnd = (result) => {
        // eslint-disable-next-line no-console
        console.log('🚀 handleDragEnd called with result:', result);

        const { source, destination } = result;

        // eslint-disable-next-line no-console
        console.log('🔄 Drag operation:', {
            draggableId: result.draggableId,
            source: source?.droppableId,
            sourceIndex: source?.index,
            destination: destination?.droppableId,
            destinationIndex: destination?.index,
        });

        // Dropped outside a droppable area
        if (!destination) {
            // eslint-disable-next-line no-console
            console.log('❌ Dropped outside droppable area');
            return;
        }

        // Source and destination lists
        const sourceList = source.droppableId;
        const destList = destination.droppableId;

        // Handle drag within same list
        if (sourceList === destList) {
            if (sourceList === 'favorites') {
                const reorderedItems = Array.from(customMenu);
                const [movedItem] = reorderedItems.splice(source.index, 1);
                reorderedItems.splice(destination.index, 0, movedItem);
                setCustomMenu(reorderedItems);
            }
        } else if (sourceList === 'mainMenu' && destList === 'favorites') {
            // Get dragged item by id from the draggableId instead of the index
            // Extract item id from the draggableId (format: "item-{id}")
            const itemId = result.draggableId.replace('item-', '');
            const mainMenuItem = menuItems.mainMenu.find((item) => item.id === itemId);

            if (mainMenuItem) {
                // Only add if it doesn't already exist in favorites
                if (!customMenu.some((item) => item.id === mainMenuItem.id)) {
                    const newCustomMenu = Array.from(customMenu);
                    newCustomMenu.splice(destination.index, 0, { ...mainMenuItem });
                    setCustomMenu(newCustomMenu);
                }
            }
        } else if (sourceList === 'mainMenu' && destList.startsWith('group-')) {
            // Handle drag from main menu to a group within favorites
            const groupId = destList.replace('group-', '');
            // Extract item id from the draggableId (format: "item-{id}")
            const itemId = result.draggableId.replace('item-', '');
            const mainMenuItem = menuItems.mainMenu.find((item) => item.id === itemId);

            if (mainMenuItem) {
                const newCustomMenu = Array.from(customMenu);
                const groupIndex = newCustomMenu.findIndex(
                    (item) => (item.id || item.key) === groupId,
                );

                if (groupIndex !== -1) {
                    // Create subMenu if it doesn't exist
                    if (!newCustomMenu[groupIndex].subMenu) {
                        newCustomMenu[groupIndex].subMenu = [];
                    }

                    // Only add if it doesn't already exist in this group
                    if (!newCustomMenu[groupIndex].subMenu.some(
                        (item) => item.id === mainMenuItem.id,
                    )) {
                        // Insert at the specific destination index instead of pushing to the end
                        newCustomMenu[groupIndex].subMenu.splice(
                            destination.index,
                            0,
                            { ...mainMenuItem },
                        );
                        setCustomMenu(newCustomMenu);
                    }
                }
            }
        } else if (sourceList === 'favorites' && destList.startsWith('group-')) {
            // Handle drag from favorites to a group
            const groupId = destList.replace('group-', '');
            const itemToMove = customMenu[source.index];

            // Don't allow dragging a group into another group
            if (itemToMove.subMenu) {
                return;
            }

            const newCustomMenu = Array.from(customMenu);
            const groupIndex = newCustomMenu.findIndex((item) => item.id === groupId);

            if (groupIndex !== -1) {
                // Create subMenu if it doesn't exist
                if (!newCustomMenu[groupIndex].subMenu) {
                    newCustomMenu[groupIndex].subMenu = [];
                }

                // Add the item at the specific destination index in the group
                newCustomMenu[groupIndex].subMenu.splice(destination.index, 0, itemToMove);
                // Remove from the top level
                newCustomMenu.splice(source.index, 1);
                setCustomMenu(newCustomMenu);
            }
        } else if (sourceList.startsWith('group-') && destList === 'favorites') {
            // Handle drag from a group to favorites (top level)
            // eslint-disable-next-line no-console
            console.log('⚠️ BLOCKED: Preventing group item from moving to favorites. This was likely an unintended drag.');
            
            // BLOCK this operation - it's likely an unintended drag due to overlapping droppable zones
            // Users should explicitly drag outside the group area if they want to move to main level
            return;
        } else if (sourceList.startsWith('group-') && destList.startsWith('group-')) {
            // Handle drag between different groups
            const sourceGroupId = sourceList.replace('group-', '');
            const destGroupId = destList.replace('group-', '');

            if (sourceGroupId === destGroupId) {
                // Handle reordering within same group
                // eslint-disable-next-line no-console
                console.log('🔀 Reordering within group:', sourceGroupId);

                const groupIndex = customMenu.findIndex(
                    (item) => (item.id || item.key) === sourceGroupId,
                );
                // eslint-disable-next-line no-console
                console.log('📍 Group index:', groupIndex, 'Group found:', groupIndex !== -1);

                if (groupIndex !== -1 && customMenu[groupIndex].subMenu) {
                    // eslint-disable-next-line no-console
                    console.log('📋 Current subMenu:', customMenu[groupIndex].subMenu);

                    // Force subMenu to be an array if it's not
                    if (!Array.isArray(customMenu[groupIndex].subMenu)) {
                        // eslint-disable-next-line no-console
                        console.log('❌ subMenu is not an array!');
                        return;
                    }

                    // Create a new copy of the custom menu
                    const newCustomMenu = [...customMenu];
                    const subMenuItems = [...newCustomMenu[groupIndex].subMenu];

                    // eslint-disable-next-line no-console
                    console.log(
                        '📊 SubMenu length:',
                        subMenuItems.length,
                        'Source index:',
                        source.index,
                        'Destination index:',
                        destination.index,
                    );

                    // Check if source index is valid
                    if (source.index >= subMenuItems.length || source.index < 0) {
                        // eslint-disable-next-line no-console
                        console.log('❌ Invalid source index');
                        return;
                    }

                    // Check if destination index is valid
                    if (destination.index < 0 || destination.index > subMenuItems.length) {
                        // eslint-disable-next-line no-console
                        console.log('❌ Invalid destination index');
                        return;
                    }

                    // Get the item we're moving
                    const [movedItem] = subMenuItems.splice(source.index, 1);
                    // eslint-disable-next-line no-console
                    console.log('📦 Moving item:', movedItem);

                    // Insert at destination position
                    subMenuItems.splice(destination.index, 0, movedItem);

                    // Update the group with the reordered items
                    newCustomMenu[groupIndex] = {
                        ...newCustomMenu[groupIndex],
                        subMenu: subMenuItems,
                    };

                    // eslint-disable-next-line no-console
                    console.log('✅ Updated subMenu:', newCustomMenu[groupIndex].subMenu);

                    // Update the state with our new menu structure
                    setCustomMenu(newCustomMenu);
                }
            } else {
                // Handle drag between different groups
                const sourceGroupIndex = customMenu.findIndex(
                    (item) => (item.id || item.key) === sourceGroupId,
                );
                const destGroupIndex = customMenu.findIndex(
                    (item) => (item.id || item.key) === destGroupId,
                );

                if (sourceGroupIndex !== -1 && destGroupIndex !== -1
                    && customMenu[sourceGroupIndex].subMenu) {
                    const newCustomMenu = Array.from(customMenu);
                    const [itemToMove] = newCustomMenu[sourceGroupIndex].subMenu
                        .splice(source.index, 1);

                    // Create subMenu if it doesn't exist
                    if (!newCustomMenu[destGroupIndex].subMenu) {
                        newCustomMenu[destGroupIndex].subMenu = [];
                    }

                    newCustomMenu[destGroupIndex].subMenu.splice(destination.index, 0, itemToMove);
                    setCustomMenu(newCustomMenu);
                }
            }
        }
    };

    const addNewGroup = () => {
        if (!newGroupName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const groupId = `custom_group_${Date.now()}`;
        const newGroup = {
            id: groupId,
            title: newGroupName,
            subMenu: [],
        };

        setCustomMenu([...customMenu, newGroup]);
        setNewGroupName('');
        setShowGroupInput(false);
        setError(null);
    };

    const editGroup = (groupId, newName) => {
        if (!newName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const newCustomMenu = customMenu.map((item) => {
            const itemId = item.id || item.key;
            if (itemId === groupId) {
                return { ...item, title: newName };
            }
            return item;
        });

        setCustomMenu(newCustomMenu);
        setEditingGroup(null);
        setError(null);
    };

    const removeItem = (itemId, groupId = null) => {
        if (groupId) {
            // Remove item from group
            const newCustomMenu = customMenu.map((item) => {
                const currentItemId = item.id || item.key;
                if (currentItemId === groupId && item.subMenu) {
                    return {
                        ...item,
                        subMenu: item.subMenu.filter((subItem) => subItem.id !== itemId),
                    };
                }
                return item;
            });
            setCustomMenu(newCustomMenu);
        } else {
            // Remove item from top level
            setCustomMenu(customMenu.filter((item) => {
                const currentItemId = item.id || item.key;
                return currentItemId !== itemId;
            }));
        }
    };

    const saveMenu = () => {
        setLoading(true);
        fetch(`${baseRestURL}/menu/customized`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ favoritesMenu: customMenu }),
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then(() => {
                setSuccess('Menu saved successfully');
                setLoading(false);
                // Refresh the menu data
                loadMenuData();
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('Error saving menu:', err);
                setError('Error saving menu. Please try again.');
                setLoading(false);
            });
    };

    const resetMenu = () => {
        // eslint-disable-next-line no-alert
        if (window.confirm('Are you sure you want to reset your menu to default?')) {
            setLoading(true);
            fetch(`${baseRestURL}/menu/reset`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({}),
            })
                .then(handleHTTPErrors)
                .then((response) => response.json())
                .then(() => {
                    setSuccess('Menu reset successfully');
                    setLoading(false);
                    // Refresh the menu data
                    loadMenuData();
                })
                .catch((err) => {
                    // eslint-disable-next-line no-console
                    console.error('Error resetting menu:', err);
                    setError('Error resetting menu. Please try again.');
                    setLoading(false);
                });
        }
    };

    // Create sequential indexes for main menu items (memoized to avoid recalculation)
    const mainMenuIndexMap = React.useMemo(() => {
        let currentIndex = 0;
        const indexMap = new Map();

        if (menuItems.mainMenuStructured) {
            menuItems.mainMenuStructured.forEach((category) => {
                if (category.subMenu) {
                    category.subMenu.forEach((item) => {
                        indexMap.set(item.id, currentIndex);
                        currentIndex += 1;
                    });
                }
            });
        }

        return indexMap;
    }, [menuItems.mainMenuStructured]);

    // eslint-disable-next-line max-len
    const renderDraggableItem = (item, index, isDraggable = true, groupId = null, isMainMenu = false) => {
        // Create a consistent, unique draggableId format
        // Always use the simple format for consistency across all drag operations
        const itemDraggableId = `item-${item.id}`;

        // Create unique key for React reconciliation
        let uniqueKey;
        if (groupId) {
            uniqueKey = `group-${groupId}-item-${item.id}`;
        } else if (isMainMenu) {
            uniqueKey = `main-${item.id}`;
        } else {
            uniqueKey = `fav-${item.id}`;
        }

        // eslint-disable-next-line no-console
        console.log(`🎯 Rendering draggable: ${item.title}`, {
            id: item.id,
            index,
            groupId,
            isMainMenu,
            uniqueKey,
            draggableId: itemDraggableId,
        });

        return (
            <Draggable
                key={uniqueKey}
                draggableId={itemDraggableId}
                index={index}
                isDragDisabled={false}
            >
                {(provided, snapshot) => {
                    if (snapshot.isDragging) {
                        // eslint-disable-next-line no-console
                        console.log(`🚀 Item is being dragged: ${item.title}`, {
                            groupId,
                            draggableId: itemDraggableId,
                        });
                    }
                    return (
                        <div
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            className={
                                snapshot.isDragging
                                    ? `${styles.menuItem} ${styles.dragging}`
                                    : styles.menuItem
                            }
                        >
                        <div className={styles.menuItemContent}>
                            <FontAwesomeIcon
                                icon={faEllipsisV}
                                className={styles.dragHandle}
                                onMouseDown={() => console.log(`🖱️ MouseDown on drag handle for: ${item.title} (groupId: ${groupId})`)}
                            />
                            <span className={styles.itemTitle}>{item.title}</span>
                            {groupId && (
                                <Button
                                    color="link"
                                    className={styles.actionButton}
                                    onClick={() => removeItem(item.id, groupId)}
                                    title="Remove from group"
                                >
                                    <FontAwesomeIcon icon={faMinus} />
                                </Button>
                            )}
                            {!groupId && !isMainMenu && (
                                <Button
                                    color="link"
                                    className={styles.actionButton}
                                    onClick={() => removeItem(item.id)}
                                    title="Remove from favorites"
                                >
                                    <FontAwesomeIcon icon={faTrash} />
                                </Button>
                            )}
                        </div>
                    </div>
                    );
                }}
            </Draggable>
        );
    };

    // Function to render menu in category columns
    const renderCategoryColumns = (menuStructure) => {
        // Split the menu structure into balanced columns
        const numColumns = 4; // Define number of columns
        const itemsPerColumn = Math.ceil(menuStructure.length / numColumns);
        const columns = [];

        // Create columns
        for (let i = 0; i < numColumns; i += 1) {
            const startIndex = i * itemsPerColumn;
            const endIndex = Math.min(startIndex + itemsPerColumn, menuStructure.length);
            const columnItems = menuStructure.slice(startIndex, endIndex);

            columns.push(
                <div key={`column-${i}`} className={styles.categoryColumn}>
                    {columnItems.map((category) => (
                        <div key={category.id} className={styles.categoryContainer}>
                            <button type="button" className={styles.categoryTitle}>
                                {category.title}
                            </button>
                            <div className="collapse show">
                                <ul className={styles.categoryLinks}>
                                    {category.subMenu && category.subMenu.map((item) => (
                                        <li
                                            key={item.id}
                                            className={styles.categoryLink}
                                        >
                                            {renderDraggableItem(
                                                item,
                                                mainMenuIndexMap.get(item.id) || 0,
                                                true,
                                                null,
                                                true,
                                            )}
                                        </li>
                                    ))}
                                    {(!category.subMenu || category.subMenu.length === 0) && (
                                        <li className={styles.categoryLink}>
                                            <div className={styles.emptyGroup}>
                                                <p>No items in this category</p>
                                            </div>
                                        </li>
                                    )}
                                </ul>
                            </div>
                        </div>
                    ))}
                </div>,
            );
        }

        return columns;
    };

    const renderGroupItem = (item, index) => {
        // eslint-disable-next-line no-console
        console.log('🗂️  Rendering group:', item);
        const groupId = item.id || item.key;
        return (
            <div key={groupId} className={`${styles.menuItem} ${styles.groupItem}`}>
                        <div className={styles.menuItemContent}>
                            <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
                            <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />

                            {editingGroup === groupId ? (
                                <div className={styles.groupEditForm}>
                                    <input
                                        type="text"
                                        className={styles.groupNameInput}
                                        value={newGroupName}
                                        onChange={(e) => setNewGroupName(e.target.value)}
                                        // eslint-disable-next-line jsx-a11y/no-autofocus
                                        autoFocus
                                    />
                                    <Button
                                        color="primary"
                                        size="sm"
                                        className={styles.saveGroupButton}
                                        onClick={() => editGroup(groupId, newGroupName)}
                                    >
                                        <FontAwesomeIcon icon={faSave} />
                                    </Button>
                                    <Button
                                        color="secondary"
                                        size="sm"
                                        onClick={() => setEditingGroup(null)}
                                    >
                                        <FontAwesomeIcon icon={faUndo} />
                                    </Button>
                                </div>
                            ) : (
                                <>
                                    <span className={styles.itemTitle}>{item.title}</span>
                                    <Button
                                        color="link"
                                        className={styles.actionButton}
                                        onClick={() => {
                                            setEditingGroup(groupId);
                                            setNewGroupName(item.title);
                                        }}
                                        title="Edit group name"
                                    >
                                        <FontAwesomeIcon icon={faPencilAlt} />
                                    </Button>
                                    <Button
                                        color="link"
                                        className={styles.actionButton}
                                        onClick={() => removeItem(groupId)}
                                        title="Remove group"
                                    >
                                        <FontAwesomeIcon icon={faTrash} />
                                    </Button>
                                </>
                            )}
                        </div>

                        <Droppable droppableId={`group-${groupId}`} type="menuItem" isCombineEnabled={false}>
                            {(providedDrop, snapshotDrop) => (
                                <div
                                    ref={providedDrop.innerRef}
                                    {...providedDrop.droppableProps}
                                    className={`${styles.groupContent} ${
                                        snapshotDrop.isDraggingOver ? styles.draggingOver : ''
                                    }`}
                                >
                                    {item.subMenu && item.subMenu.map((subItem, subIndex) => {
                                        // eslint-disable-next-line no-console
                                        console.log(
                                            `🏷️  Rendering group item: ${subItem.title} in group ${groupId}`,
                                        );
                                        return renderDraggableItem(
                                            subItem,
                                            subIndex,
                                            true,
                                            groupId,
                                        );
                                    })}
                                    {providedDrop.placeholder}
                                    {(!item.subMenu || item.subMenu.length === 0) && (
                                        <div className={styles.emptyGroup}>
                                            <p>Empty group.</p>
                                            <p>
                                                Drag items here from available items or your menu.
                                            </p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </Droppable>
            </div>
        );
    };

    if (loading) {
        return <LoadingContainer />;
    }

    return (
        <div className={styles.menuCustomizer}>
            <h2>Customize Your Menu</h2>
            {error && <Alert color="danger" timeout={5000}>{error}</Alert>}
            {success && <Alert color="success" timeout={5000}>{success}</Alert>}

            <div className={styles.instructionsBox}>
                <p>
                    Customize your menu by dragging items from the available menu
                    items on the left to your custom menu on the right.
                </p>
                <ul>
                    <li>Click and drag items from the available menu to your custom menu</li>
                    <li>Create groups to organize your menu items</li>
                    <li>Drag items into groups or between groups</li>
                    <li>Rearrange items in your custom menu by dragging them</li>
                    <li>Remove items from your custom menu with the trash icon</li>
                    <li>Save your changes when you&apos;re finished customizing</li>
                </ul>
            </div>

            <div className={styles.menuContainer}>
                <DragDropContext 
                    onDragStart={(start) => {
                        console.log('🟡 Drag started:', start);
                    }}
                    onDragUpdate={(update) => {
                        console.log('🔵 Drag update:', update);
                    }}
                    onDragEnd={handleDragEnd}
                    onBeforeCapture={(before) => {
                        console.log('🔍🔍🔍 BEFORE CAPTURE:', JSON.stringify(before, null, 2));
                    }}
                    onBeforeDragStart={(initial) => {
                        console.log('📌📌📌 BEFORE DRAG START:', JSON.stringify(initial, null, 2));
                    }}
                >
                    <div className={styles.menuSection}>
                        <Card>
                            <CardHeader>Available Menu Items</CardHeader>
                            <CardBody>
                                <Droppable droppableId="mainMenu" type="menuItem">
                                    {(providedMain) => (
                                        <div
                                            ref={providedMain.innerRef}
                                            {...providedMain.droppableProps}
                                            className={`${styles.menuList} ${styles.mainMenuList}`}
                                        >
                                            {/* eslint-disable-next-line max-len */}
                                            {menuItems.mainMenuStructured && renderCategoryColumns(menuItems.mainMenuStructured)}
                                            {providedMain.placeholder}
                                        </div>
                                    )}
                                </Droppable>
                            </CardBody>
                        </Card>
                    </div>

                    <div className={styles.menuControls}>
                        <div className={styles.controlArrow}>
                            <FontAwesomeIcon icon={faArrowDown} />
                            <FontAwesomeIcon icon={faArrowUp} />
                        </div>
                    </div>

                    <div className={styles.menuSection}>
                        <Card>
                            <CardHeader>
                                Your Custom Menu
                                <div className={styles.headerActions}>
                                    {!showGroupInput ? (
                                        <Button
                                            color="primary"
                                            size="sm"
                                            onClick={() => setShowGroupInput(true)}
                                            title="Add a new group"
                                        >
                                            <FontAwesomeIcon icon={faPlus} />
                                            <span>Add Group</span>
                                        </Button>
                                    ) : (
                                        <div className={styles.groupForm}>
                                            <input
                                                type="text"
                                                className={styles.groupNameInput}
                                                value={newGroupName}
                                                onChange={(e) => setNewGroupName(e.target.value)}
                                                placeholder="Group name"
                                                // eslint-disable-next-line jsx-a11y/no-autofocus
                                                autoFocus
                                            />
                                            <Button
                                                color="primary"
                                                size="sm"
                                                onClick={addNewGroup}
                                            >
                                                Add
                                            </Button>
                                            <Button
                                                color="secondary"
                                                size="sm"
                                                onClick={() => {
                                                    setShowGroupInput(false);
                                                    setNewGroupName('');
                                                }}
                                            >
                                                Cancel
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            </CardHeader>
                            <CardBody>
                                <Droppable droppableId="favorites" type="menuItem">
                                    {(providedFav) => (
                                        <div
                                            ref={providedFav.innerRef}
                                            {...providedFav.droppableProps}
                                            className={styles.menuList}
                                        >
                                            {customMenu.map((item, index) => {
                                                // eslint-disable-next-line no-console
                                                console.log(`📋 Rendering favorites item: ${item.title} at index ${index}`);
                                                return item.subMenu
                                                    ? renderGroupItem(item, index)
                                                    : renderDraggableItem(
                                                        item,
                                                        index,
                                                        true,
                                                        null,
                                                        false,
                                                    );
                                            })}
                                            {providedFav.placeholder}
                                            {customMenu.length === 0 && (
                                                <div className={styles.emptyMenu}>
                                                    <p>Your custom menu is empty.</p>
                                                    <p>Drag items from available menu items.</p>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </Droppable>
                            </CardBody>
                        </Card>
                    </div>
                </DragDropContext>
            </div>

            <div className={styles.actionButtons}>
                <Button color="primary" onClick={saveMenu}>
                    <FontAwesomeIcon icon={faSave} />
                    <span>Save Changes</span>
                </Button>
                <Button color="secondary" onClick={resetMenu}>
                    <FontAwesomeIcon icon={faUndo} />
                    <span>Reset to Default</span>
                </Button>
            </div>
        </div>
    );
}

export default MenuCustomizer;
