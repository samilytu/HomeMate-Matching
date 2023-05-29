package com.samilozturk.homemate_matching.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
@Parcelize
value class UniqueId(val value: String) : Parcelable