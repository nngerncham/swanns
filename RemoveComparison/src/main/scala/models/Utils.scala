package muic.nawat.senior.rmcomp
package models

import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}

def distance(p1: Point, p2: Point): Float =
  Math.sqrt(p1.zip(p2).map({ case (a, b) => Math.pow(a - b, 2) }).sum).toFloat

def computeMedoidIdx(points: Vector[Point]): Int = {
  // takes O(n^2) time but whatever
  val n = points.length
  val distances = (0 until n).zip(points).map({ case (index, p1) =>
    (index, points.map(p2 => distance(p1, p2)).sum)
  })
  val (minIndex, _) = distances.minBy(_._2)
  minIndex
}

def readFvecIntoPoints(fvecFilePath: String): Vector[Point] = {
  val rawBytes = Files.readAllBytes(Paths.get(fvecFilePath))
  val dims = ByteBuffer.wrap(rawBytes.take(4)).order(ByteOrder.LITTLE_ENDIAN).getInt
  val rawFloats = rawBytes.grouped(4).map(byteArr => {
    ByteBuffer.wrap(byteArr).order(ByteOrder.LITTLE_ENDIAN).getFloat
  }).toVector
  rawFloats.grouped(dims + 1).map(_.tail.toArray).toVector
}
