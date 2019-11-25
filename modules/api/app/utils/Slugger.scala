package utils

import java.text.Normalizer
import java.util.Locale

object Slugger {
  def toSlug(input: String): String = {
    Option(input)
      .map(_.trim)
      .map(normalize _ andThen removeSpaces andThen hyphenate andThen toLowercase)
      .getOrElse("")
  }

  def toNormalized(input: String): String = {
    Option(input)
      .map(normalize)
      .getOrElse("")
  }

  def CapitalizeAll(input: String): String = {
    removePunctuation(input).toLowerCase.split(" ").map(_.capitalize).mkString(" ")

  }
  def Capitalize(input: String): String = {
    removePunctuation(input).toLowerCase.capitalize

  }

  private def toLowercase(input: String) = input.toLowerCase(Locale.FRANCE)

  private def toUppercase(input: String) = input.toUpperCase(Locale.FRANCE)

  private def removeSpaces(input: String): String = input.replaceAll("\\s+", " ")

  private def removePunctuation(input: String): String = input.replaceAll("\\P{Alpha}", " ")

  private def hyphenate(input: String): String = {
    val text = input.replaceAll("\\P{Alnum}", "-").replaceAll("-+", "-")

    if (text.endsWith("-")) text.dropRight(1) else text
  }

  private def normalize(input: String): String =
    Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
}
