package com.farywave.memehive.data.local.db.entity

data class Collection(val id: Int, var name: String, var logoId: Int, val content: MutableList<Int>) {
    override fun equals(other: Any?): Boolean {
        return other is Collection && other.id == id
    }
}