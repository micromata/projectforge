package org.projectforge.rest.poll.types


val PREMADE_QUESTIONS = mapOf(
    "HAS_FOOD" to Question(
        question = "Was willst du essen?",
        type = BaseType.MultipleChoices,
        answers = mutableListOf("Fleisch", "Vegetarisch", "Vegan"),
        numberOfSelect = 1
    ),
    "IS_REMOTE" to Question(
        question = "Nimmst du remote teil?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "CAN_HAVE_COMPANIONS" to Question(
        question = "Nimmst du eine Begleitung mit? (Name der Begleitung)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_HAVE_CHILDREN" to Question(
        question = "Nimmst du ein Kind mit? (Name der Begleitung)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        question = "Willst du dort übernachten?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "HAS_BREAKFAST" to Question(
        question = "Willst du am nächsten Tag frühstücken?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
)
