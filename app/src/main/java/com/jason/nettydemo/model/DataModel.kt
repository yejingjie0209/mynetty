package com.jason.nettydemo.model

import java.io.Serializable

data class DataModel(
    var data: String,
    var sendTime: Long,
    var index: Int): Serializable