package com.lollypop.language

import com.lollypop.language.InsertValues.InsertSourceTemplateTag
import com.lollypop.language.TemplateProcessor.tags.TemplateTag
import com.lollypop.runtime.instructions.queryables.RowsOfValues

trait InsertValues { self: LanguageParser =>

  // add custom tag for insert values (queries, values and variables)? (e.g. "%V:data" => "(select ...)" | "values (...)" | "@numbers")
  TemplateProcessor.addTag("V", InsertSourceTemplateTag)

}

object InsertValues {

  /**
   * Insert Source Template Tag
   * @param name the name of the property
   */
  case class InsertSourceTemplateTag(name: String) extends TemplateTag {
    override def extract(stream: TokenStream)(implicit compiler: SQLCompiler): SQLTemplateParams = {
      SQLTemplateParams(instructions = Map(name -> (RowsOfValues.parseQueryable(stream) || dieNoResultSet())))
    }

    override def toCode: String = s"%V:$name"
  }

}