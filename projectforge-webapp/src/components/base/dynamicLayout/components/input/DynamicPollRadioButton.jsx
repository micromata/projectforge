import PropTypes from 'prop-types';
import React from 'react';
import RadioButton from '../../../../design/input/RadioButton';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

/**
 * DynamicPollRadioButton - A specialized radio button component for Poll questions
 *
 * WHY THIS COMPONENT EXISTS:
 * ==========================
 *
 * The standard DynamicRadioButton component expects a single value (string/number) to be stored
 * in the data model. However, Poll questions use a different data structure:
 *
 * - Single-Choice Poll Questions: Store answers as a Boolean array, e.g., [false, true, false]
 *   where only ONE element can be true at a time
 * - The radio button's "checked" state must be derived from the Boolean value at a specific index
 *
 * PROBLEM WITH STANDARD COMPONENT:
 * =================================
 *
 * The standard DynamicRadioButton uses:
 *   const checked = Object.getByString(data, id) === value;
 *
 * This works for simple values like:
 *   data.gender === "male"  // true or false
 *
 * But fails for Poll questions where:
 *   data.responses[0].answers[1] === true  // Boolean in array, not a single value
 *
 * SOLUTION:
 * =========
 *
 * This component handles Boolean array values correctly:
 * 1. Reads the Boolean value at the specific index (e.g., responses[0].answers[1])
 * 2. Checks if it's true (checked) or false (unchecked)
 * 3. When clicked, sets the clicked index to true and all others to false
 *
 * USAGE:
 * ======
 *
 * Used exclusively in PollResponsePageRest.kt for rendering Single-Choice poll questions:
 *
 * ```kotlin
 * UIRadioButton(
 *     id = "responses[$index].answers[$index2]",  // Points to Boolean in array
 *     name = "single-$index",                      // Groups radio buttons
 *     value = "true",                              // Value to set when checked
 *     label = answer
 * )
 * ```
 *
 * TECHNICAL DETAILS:
 * ==================
 *
 * - The component reads the current Boolean value from the data model
 * - checked = currentValue === true (Boolean comparison, not string comparison)
 * - When a radio button is clicked, it sets its value to true
 * - React's radio button grouping (via 'name' prop) ensures only one can be selected
 *
 * DEPENDENCIES:
 * =============
 *
 * - useMemo dependencies are carefully chosen to prevent unnecessary re-renders
 * - Excludes 'props' object to avoid re-render on every parent update
 * - Includes specific props (name, label) that affect rendering
 *
 * @author m.nuhn@micromata.de
 * @since 2026-02-05
 */
function DynamicPollRadioButton(
    {
        id,
        type,
        value,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    // Read the Boolean value from the data model
    // For Poll questions, this will be something like: data.responses[0].answers[1]
    const currentValue = Object.getByString(data, id);

    // Check if this radio button should be checked
    // We compare with true (Boolean) because Poll answers are stored as Booleans
    const checked = currentValue === true;

    return React.useMemo(() => {
        const handleRadioButtonChange = ({ target }) => {
            if (target.checked) {
                // For Single-Choice: set ALL other options to false
                // Parse ID: "responses[1].answers[2]" -> questionIndex = 1
                const match = id.match(/responses\[(\d+)\]\.answers\[(\d+)\]/);
                if (match) {
                    const questionIndex = parseInt(match[1], 10);
                    const answerIndex = parseInt(match[2], 10);

                    // Get current answers array for this question
                    const currentAnswers = data.responses?.[questionIndex]?.answers || [];

                    // Create new array: all false, only clicked one is true
                    const newAnswers = currentAnswers.map((_, idx) => idx === answerIndex);

                    // Set the entire answers array at once
                    setData({ [`responses[${questionIndex}].answers`]: newAnswers });
                } else {
                    // Fallback: If regex doesn't match, use old behavior
                    setData({ [id]: true });
                }
            }
        };

        return (
            <DynamicValidationManager id={id}>
                <RadioButton
                    id={`${ui.uid}-${id}-${value}`}
                    checked={checked}
                    onChange={handleRadioButtonChange}
                    {...props}
                />
            </DynamicValidationManager>
        );
    }, [checked, setData, id, value, ui.uid, props.name, props.label, data.responses]);
}

DynamicPollRadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

export default DynamicPollRadioButton;
