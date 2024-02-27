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

def computeMedoidIdx(
  points: Vector[Point]
)(implicit samplingRatio: Float = 0.33): Int = {
  // takes O(n^2) time but whatever
  val n          = points.length
  val sampleSize = (n * samplingRatio).toInt
  val sample     = Random.shuffle(points.zipWithIndex).take(sampleSize)

  val distancesFuture =
    Future.sequence(sample.map({ case (p1, i) =>
      Future {
        val dist = sample.map({ case (p2, _) => distance(p1, p2) }).sum
        (i, dist)
      }
    }))
  val distances     = Await.result(distancesFuture, Duration.Inf)
  val (minIndex, _) = distances.minBy(_._2)
  minIndex
}

def readFvecIntoPoints(fvecFilePath: String): Vector[Point] = {
  val rawBytes = Files.readAllBytes(Paths.get(fvecFilePath))
  val dims =
    ByteBuffer.wrap(rawBytes.take(4)).order(ByteOrder.LITTLE_ENDIAN).getInt
  val rawFloats = rawBytes
    .grouped(4)
    .map(byteArr => {
      ByteBuffer.wrap(byteArr).order(ByteOrder.LITTLE_ENDIAN).getFloat
    })
    .toVector
  rawFloats.grouped(dims + 1).map(_.tail.toArray).toVector
}

def computeGroundTruths(
  dataset: Vector[Point],
  queryData: Vector[Point],
  k: Int
): Vector[Array[(Int, Float)]] = {
//  val kNNFutures = Future.sequence(
//    queryData.map(queryPoint =>
//      Future {
//        dataset.zipWithIndex
//          .map({ case (basePoint, i) => (i, distance(queryPoint, basePoint)) })
//          .sortBy(_._2)
//          .take(k)
//          .toArray
//      }
//    )
//  )
//  Await.result(kNNFutures, Duration.Inf)
  queryData.map(queryPoint =>
    dataset.zipWithIndex
      .map({ case (basePoint, i) => (i, distance(queryPoint, basePoint)) })
      .sortBy(_._2)
      .take(k)
      .toArray
  )
}

def computeRecall(
  indexSearchResult: Vector[Array[(Int, Float)]],
  groundTruth: Vector[Array[(Int, Float)]]
) = {
  val recalls = indexSearchResult
    .zip(groundTruth)
    .map({ case (index, truth) =>
      val indexSet     = index.map(_._1).toSet
      val truthSet     = truth.map(_._1).toSet
      val intersection = indexSet.intersect(truthSet)
      intersection.size.toFloat / truthSet.size
    })
  recalls.sum.toDouble / recalls.length.toDouble
}
