package muic.nawat.senior.rmcomp
package models

import scala.annotation.targetName
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.Ordering

class DiskANNBase(
  val searchSize: Int,
  val degreeBound: Int,
  val alpha: Float,
  val tracker: Tracker,
  val maxIndexSize: Int
) extends ANNSGraphBase
    with Serializable {
  var startingIdx: Int                = -1
  var points: Vector[Point]           = Vector.empty
  var neighborhoods: Vector[Set[Int]] = Vector.empty

  @targetName("equalsDiskANN")
  def ==(other: DiskANNBase): Boolean = {
    this.startingIdx == other.startingIdx &&
    this.searchSize == other.searchSize &&
    this.degreeBound == other.degreeBound &&
    this.alpha == other.alpha &&
    this.maxIndexSize == other.maxIndexSize &&
//    this.points == other.points
    this.neighborhoods == other.neighborhoods
  }

  private def robustPrune(pointIdx: Int, candidates: Vector[Int]): Set[Int] = {
    val candidateIndexes =
      (candidates ++ neighborhoods(pointIdx)).toSet - pointIdx
    val candidatePairFutures = Future.sequence(
      candidateIndexes.map(idx =>
        Future { (idx, distance(points(pointIdx), points(idx))) }
      )
    )
    val candidatePQ = mutable.PriorityQueue.empty[(Int, Float)](
      Ordering.by[(Int, Float), Float](-_._2)
    )
    Await
      .result(candidatePairFutures, Duration.Inf)
      .foreach(candidatePQ.enqueue(_))

    var newNeighborhood = Set[Int]()

    while (candidatePQ.nonEmpty) {
      val (pStarIdx, pStarDist) = candidatePQ.dequeue()
      newNeighborhood = newNeighborhood + pStarIdx
      if (newNeighborhood.size >= degreeBound) {
        return newNeighborhood
      }

      candidatePQ.filter({ case (pPrimeIdx, pPrimeDist) =>
        alpha * distance(points(pStarIdx), points(pPrimeIdx)) > pPrimeDist
      })
    }

    newNeighborhood
  }

  private def greedySearch(
    query: Point,
    k: Int
  ): (Array[(Int, Float)], Vector[Int]) = {
    var beam = mutable.PriorityQueue.empty[(Int, Float)](
      Ordering.by[(Int, Float), Float](-_._2)
    )
    beam.enqueue((startingIdx, distance(points(startingIdx), query)))
    val visited = mutable.Set[Int]()

    while (beam.nonEmpty) {
      val (pStarIdx, pStarDist) = beam.dequeue()

      neighborhoods(pStarIdx).foreach(pPrimeIdx => {
        if (!visited.contains(pPrimeIdx)) {
          beam.enqueue((pPrimeIdx, distance(points(pPrimeIdx), query)))
        }
      })
      visited += pStarIdx

      if (beam.size > searchSize) {
        beam = beam.take(searchSize)
      }
    }

    visited.foreach(v => {
      beam.enqueue((v, distance(points(v), query)))
    })
    (beam.take(k).toArray, visited.toVector)
  }

  override def batchAdd(newPoints: Seq[Point]): Unit = {
    if (this.points.size + newPoints.length > maxIndexSize) {
      return
    }

    if (this.points.isEmpty) {
      this.points = this.points ++ newPoints

      // initializes G as a random R-regular graph
      val rng = new scala.util.Random()
      val nbrsFuture = Future.sequence(this.points.indices.map({ idx =>
        Future {
          var neighborhood = Set[Int]()
          while (neighborhood.size < degreeBound) {
            val neighborIdx = rng.nextInt(this.points.size)
            if (neighborIdx != idx) {
              neighborhood = neighborhood + neighborIdx
            }
          }
          neighborhood
        }
      }))
      this.neighborhoods = Await.result(nbrsFuture, Duration.Inf).toVector
      println("Random graph initialized")

      this.startingIdx = computeMedoidIdx(this.points)
      println("Medoid computed")

      for (_ <- 0 until 2) {
        val pruningNeighborhoods = Future.sequence(
          this.points.indices
            .zip(this.points)
            .map({ case (pointIdx, point) =>
              Future {
                val (candidates, path) = greedySearch(point, 1)
                val prunedCandidates   = robustPrune(pointIdx, path)
                synchronized {
                  this.neighborhoods =
                    this.neighborhoods.updated(pointIdx, prunedCandidates)
                }
              }
            })
        )
        Await.result(pruningNeighborhoods, Duration.Inf)
        this.points.indices
          .zip(this.points)
          .map({ case (pointIdx, point) =>
            Future {
              this
                .neighborhoods(pointIdx)
                .foreach(nbrIdx => {
                  if (
                    (this.neighborhoods(nbrIdx) + pointIdx).size > degreeBound
                  ) {
                    synchronized {
                      this.neighborhoods = this.neighborhoods
                        .updated(
                          nbrIdx,
                          robustPrune(
                            nbrIdx,
                            (this.neighborhoods(nbrIdx) + pointIdx).toVector
                          )
                        )
                    }
                  } else {
                    synchronized {
                      this.neighborhoods = this.neighborhoods
                        .updated(nbrIdx, this.neighborhoods(nbrIdx) + pointIdx)
                    }
                  }
                })
            }
          })
        //      this.points.zipWithIndex
        //        .foreach({ case (point, pointIdx) =>
        //          val (candidates, path) = greedySearch(point, 1)
        //          this.neighborhoods =
        //            this.neighborhoods.updated(pointIdx, robustPrune(pointIdx, path))
        //
        //          this
        //            .neighborhoods(pointIdx)
        //            .foreach(neighborIdx => {
        //              if (
        //                (this.neighborhoods(neighborIdx) + pointIdx).size > degreeBound
        //              ) {
        //                this.neighborhoods = this.neighborhoods.updated(
        //                  neighborIdx,
        //                  robustPrune(
        //                    neighborIdx,
        //                    (this.neighborhoods(neighborIdx) + pointIdx).toVector
        //                  )
        //                )
        //              } else {
        //                this.neighborhoods = this.neighborhoods.updated(
        //                  neighborIdx,
        //                  this.neighborhoods(neighborIdx) + pointIdx
        //                )
        //              }
        //            })
        //        })
      }
      println("Fixing out-neighborhoods done")

    } else {
//      val startingN = this.points.size
//      this.points = this.points ++ newPoints
//
//      newPoints.indices
//        .zip(newPoints)
//        .foreach({ case (idxSurplus, point) =>
//          val (candidates, path) = greedySearch(point, 1)
//          val prunedCandidates   = robustPrune(startingN + idxSurplus, path)
//
//          prunedCandidates.foreach({ case (neighborIdx, neighborDist) =>
//            this
//              .neighborhoods(neighborIdx)
//              .enqueue((startingN + idxSurplus, neighborDist))
//
//            if (this.neighborhoods(neighborIdx).size > degreeBound) {
//              val neighborhoodToPrune = this
//                .neighborhoods(neighborIdx)
//                .map({ case (idx, _) => idx })
//                .toVector :+ (startingN + idxSurplus)
//              val prunedNeighborhood =
//                robustPrune(neighborIdx, neighborhoodToPrune)
//              this.neighborhoods =
//                this.neighborhoods.updated(neighborIdx, prunedNeighborhood)
//            }
//          })
//        })
    }
  }

  override def search(query: Point, k: Int): Array[(Int, Float)] = {
    val (result, _) = greedySearch(query, k)
    result
  }

  override def saveIndex(path: String): Unit = {
    val oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))
    oos.writeObject(this)
    oos.close()
  }
}

object DiskANNBase {
  def loadIndex(path: String): DiskANNBase = {
    val ois   = new java.io.ObjectInputStream(new java.io.FileInputStream(path))
    val index = ois.readObject().asInstanceOf[DiskANNBase]
    ois.close()
    index
  }
}
