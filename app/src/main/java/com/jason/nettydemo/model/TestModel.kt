package com.jason.nettydemo.model

import java.io.Serializable

/**
 * @author Jason
 * @description:
 * @date :2019-05-15 17:12
 */
data class TestModel(var b: ByteArray, var time: String) : Serializable{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestModel

        if (!b.contentEquals(other.b)) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = b.contentHashCode()
        result = 31 * result + time.hashCode()
        return result
    }
}