package muic.nawat.senior.rmcomp
package models

import java.nio.file.{Files, Paths}
import java.nio.{ByteBuffer, ByteOrder}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Random

def distance(p1: Point, p2: Point): Float = {
  //  val distanceFutures = Future.sequence(p1.zip(p2).map({ case (a, b) => Future(Math.pow(a - b, 2)) }))
  //  val sumDistance = Await.result(distanceFutures, Duration.Inf).sum
  //  Math.sqrt(sumDistance).toFloat
  Math.sqrt(p1.zip(p2).map({ case (a, b) => Math.pow(a - b, 2) }).sum).toFloat
}

def computeMedoidIdx(points: Vector[Point])(implicit samplingRatio: Float = 0.1): Int = {
  // takes O(n^2) time but whatever
  val n = points.length
  val sampleSize = (n * samplingRatio).toInt
  val sample = Random.shuffle(points).take(sampleSize)

  val distancesFuture = Future.sequence(sample.zipWithIndex.map({ case (p1, i) =>
    Future {
      val dist = sample.map(p2 => distance(p1, p2)).sum
      (i, dist)
    }
  }))
  val distances = Await.result(distancesFuture, Duration.Inf)
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
