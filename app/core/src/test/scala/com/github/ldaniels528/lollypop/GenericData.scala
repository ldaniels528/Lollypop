package com.github.ldaniels528.lollypop

import com.lollypop.runtime.ColumnInfo

import scala.annotation.meta.field

case class GenericData(@(ColumnInfo@field)(typeDef = "RowNumber") _id: Long = 0L,
                       @(ColumnInfo@field)(maxSize = 5) idValue: String,
                       @(ColumnInfo@field)(maxSize = 5) idType: String,
                       responseTime: Int,
                       reportDate: Long)
