package com.samilozturk.homemate_matching.data.model.db

import com.google.firebase.firestore.DocumentId
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.UniqueId

data class MatchRequestDbModel(
    @DocumentId val uid: String = "",
    val status: Status = Status.RECEIVED,
    val targetUser: UserDbModel = UserDbModel(),
) {
    enum class Status {
        SENT,
        RECEIVED,
        MATCHED,
    }
}

fun MatchRequestDbModel.toMatchRequest(): MatchRequest {
    return when (status) {
        MatchRequestDbModel.Status.SENT -> MatchRequest.Sent(
            uid = UniqueId(uid),
            targetStudent = targetUser.toStudent()
        )

        MatchRequestDbModel.Status.RECEIVED -> MatchRequest.Received(
            uid = UniqueId(uid),
            targetStudent = targetUser.toStudent()
        )

        MatchRequestDbModel.Status.MATCHED -> MatchRequest.Matched(
            uid = UniqueId(uid),
            targetStudent = targetUser.toStudent()
        )
    }
}