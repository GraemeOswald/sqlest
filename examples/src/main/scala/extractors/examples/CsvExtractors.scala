package sqlest.examples

object CSVDefinitions {
  // Start with some type aliases for CSV data
  type CSVRow = List[String]
  type CSV = List[CSVRow]

  // Now define a simple parser
  def parse(input: String): CSV =
    input.split(scala.util.Properties.lineSeparator).toList.map(_.split(",").toList)

  // Sample contents of a csv file containing person information
  val csvFile = """
    |Anne,35,1,Old Kent Road,
    |Bob,45,2,Whitechapel,
    |Charlie,20,,Lost,
    """.trim.stripMargin

  // Parse it into our CSV type
  val parsedCsv: CSV = parse(csvFile)
}

import CSVDefinitions._
import sqlest.extractor.{ CellExtractor, ExtractorSyntax }

// Implement the CellExtractor trait for CSVRow that will read a String
case class StringExtractor(index: Int) extends CellExtractor[CSVRow, String] {
  // The read method returns an Option of the read value to allow for null values
  def read(row: CSVRow): Option[String] = {
    val cellValue = row(index)
    if (cellValue.trim.nonEmpty) Some(cellValue)
    else None
  }
}

// Implement the CellExtractor trait for CSVRow that will read a Int
case class IntExtractor(index: Int) extends CellExtractor[CSVRow, Int] {
  def read(row: CSVRow): Option[Int] = scala.util.Try(Integer.parseInt(row(index))).toOption
}

// Extend the application with ExtractorSyntax for the row type
// This provides the methods `extract`, `extractTuple` and `extractConstant`
object CSVExtractorExample extends App with ExtractorSyntax[CSVRow] {
  // Create some cell extractors
  val nameExtractor = StringExtractor(0)
  val ageExtractor = IntExtractor(1)
  val houseExtractor = IntExtractor(2)
  val streetExtractor = StringExtractor(3)

  // Create a tuple extractor that will read all fields
  val tupleExtractor =
    extractTuple(
      nameExtractor,
      ageExtractor,
      houseExtractor.asOption, // Handle the possibility that this cell can contain a null value
      streetExtractor.asOption
    )

  // `extractHeadOption` and `extractAll` must be passed an `Iterable[CSVRow]`
  // `List[CSVRow]` implements `Iterable[CSVRow]` so parsedCsv can be used directly

  // extractHeadOption tries to read the first row. If there isn't one it returns None
  println(tupleExtractor.extractHeadOption(parsedCsv))
  // => Some((Anne, 35, Some(1), Some(Old Kent Road)))

  // extractAll reads all rows into a List
  println(tupleExtractor.extractAll(parsedCsv))
  /* =>
    List(
      (Anne, 35, Some(1), Some(Old Kent Road)),
      (Bob, 45, Some(2), Some(Whitechapel)),
      (Charlie, 20, None, Some(Lost)))
  */

  // Define the domain classes to extract from the CSV data
  case class Person(name: String, age: Int, address: Option[Address])
  case class Address(house: Int, street: String)

  // Create extractors to read the domain classes
  val addressExtractor = extract[Address](houseExtractor, streetExtractor)

  val addressExtractor2 = extractCaseClass(houseExtractor, streetExtractor)(Address.apply)

  // Named arguments often enhance the readibility of extractor definitions
  val personExtractor = extract[Person](
    name = nameExtractor,
    age = ageExtractor,
    address = addressExtractor.asOption
  ) // If any value read by the addressExtractor is null, None will be returned

  val personExtractor2 = extractCaseClass(
    nameExtractor,
    ageExtractor,
    addressExtractor2.asOption
  )(Person.apply)

  println(addressExtractor.extractHeadOption(parsedCsv))
  // => Some(Address(1, Old Kent Road))

  println(personExtractor.extractAll(parsedCsv))
  /* =>
    List(
      Person(Anne, 35, Some(Address(1, Old Kent Road))),
      Person(Bob, 45, Some(Address(2, Whitechapel))),
      Person(Charlie, 20, None))
  */
}
