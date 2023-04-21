package org.projectforge.rest.poll.types

import java.util.UUID


val PREMADE_QUESTIONS = mapOf(
    "HAS_FOOD" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Was willst du essen?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Fleisch", "Vegetarisch", "Vegan")
    ),
    "IS_REMOTE" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du remote teil?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "CAN_HAVE_COMPANIONS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du eine Begleitung mit? (Name der Begleitung)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_HAVE_CHILDREN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du ein Kind mit? (Name der Begleitung)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du dort übernachten?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "HAS_BREAKFAST" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du am nächsten Tag frühstücken?",
        type = BaseType.YesNoQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
)
