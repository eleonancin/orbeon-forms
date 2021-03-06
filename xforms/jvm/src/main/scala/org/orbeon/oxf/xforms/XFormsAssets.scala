/**
  * Copyright (C) 2017 Orbeon, Inc.
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the
  * GNU Lesser General Public License as published by the Free Software Foundation; either version
  *  2.1 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  */
package org.orbeon.oxf.xforms

import org.orbeon.oxf.properties.Properties
import org.orbeon.oxf.util.CoreUtils._
import org.orbeon.oxf.util.PathUtils._
import spray.json._

case class XFormsAssets(css: List[AssetPath], js: List[AssetPath])

case class AssetPath(full: String, minOpt: Option[String]) {
  def assetPath(tryMin: Boolean): String =
    if (tryMin) minOpt getOrElse full else full
}

object AssetPath {

  def apply(full: String, hasMin: Boolean): AssetPath =
    AssetPath(full, hasMin option minFromFull(full))

  def minFromFull(full: String): String =
    findExtension(full) match {
      case Some(ext) ⇒ full.substring(0, full.size - ext.size - 1) + ".min." + ext
      case None      ⇒ throw new IllegalArgumentException
    }
}

object XFormsAssets {

  val AssetsBaselineProperty = "oxf.xforms.assets.baseline"

  private def assetsFromJSON(json: JsValue): XFormsAssets = {

    def collectFullMin(key: String, fields: Map[String, JsValue]) =
      fields.get(key) match {
        case Some(JsArray(values)) ⇒
          values collect { case JsObject(fields) ⇒
            val full   = fields.get("full") collect { case JsString(v)  ⇒ v } getOrElse (throw new IllegalArgumentException)
            val hasMin = fields.get("min")  collect { case JsBoolean(v) ⇒ v } getOrElse false

            AssetPath(full, hasMin)
          }
        case _ ⇒ throw new IllegalArgumentException
      }

    json match {
      case JsObject(fields) ⇒

        val css = collectFullMin("css", fields)
        val js  = collectFullMin("js",  fields)

        XFormsAssets(css.to[List], js.to[List])

      case _ ⇒ throw new IllegalArgumentException
    }
  }

  def assetsFromString(json: String): XFormsAssets = assetsFromJSON(json.parseJson)

  private def properties = Properties.instance.getPropertySet

  def fromJSONProperty: XFormsAssets = {
    val property = properties.getPropertyOrThrow(AssetsBaselineProperty)
    property.associatedValue(v ⇒ assetsFromString(v.value.toString))
  }
}
