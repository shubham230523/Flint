package com.shubhamthorat.flint.domain.model

import com.shubhamthorat.flint.domain.repository.ContentAsset

data class Campaign(
    val id: String,
    val title: String,
    val ideaOrSource: String,
    val items: List<ContentAsset>,
    val createdAtTimestamp: Long = 0L
)
