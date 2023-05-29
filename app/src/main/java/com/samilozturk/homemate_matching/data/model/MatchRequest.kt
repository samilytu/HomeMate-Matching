package com.samilozturk.homemate_matching.data.model

sealed interface MatchRequest {
    val uid: UniqueId
    val targetStudent: Student

    data class Sent(
        override val uid: UniqueId,
        override val targetStudent: Student
    ) : MatchRequest

    data class Received(
        override val uid: UniqueId,
        override val targetStudent: Student
    ) : MatchRequest

    data class Matched(
        override val uid: UniqueId,
        override val targetStudent: Student
    ) : MatchRequest

}