package com.nfl.dm.shield.graphql.domain.graph.annotation

enum class ArgumentType private constructor(postFix: String) {
    QUERY(""),
    QUERY_CONTAINS("CONTAINS"),
    QUERY_CUSTOM(""),
    ORDER_BY(""),
    ORDER_DIRECTION("");

    var postFix: String
        internal set

    init {
        this.postFix = postFix
    }
}
