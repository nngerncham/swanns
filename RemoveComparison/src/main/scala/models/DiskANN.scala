package muic.nawat.senior.rmcomp
package models

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.Ordering

@SerialVersionUID(100L)
class DiskANN(
  var searchSize: Int,
  var degreeBound: Int,
  var alpha: Float,
  var tracker: Tracker,
  var maxIndexSize: Int
) extends ANNSGraph
    with Serializable {
  var startingIdx: Int                                       = -1
  var points: Vector[Point]                                  = Vector.empty
  var neighborhoods: Vector[mutable.PriorityQueue[Neighbor]] = Vector.empty

  def this(pathToIndex: String) = {
    this(0, 0, 0, new Tracker(), 1)
    val oi =
      new java.io.ObjectInputStream(new java.io.FileInputStream(pathToIndex))
    val index = oi.readObject().asInstanceOf[DiskANN]

    this.startingIdx = index.startingIdx
    this.points = index.points
    this.neighborhoods = index.neighborhoods
    this.searchSize = index.searchSize
    this.degreeBound = index.degreeBound
    this.alpha = index.alpha
    this.tracker = index.tracker
    this.maxIndexSize = index.maxIndexSize
  }

  def ==(other: DiskANN): Boolean = {
    this.startingIdx == other.startingIdx &&
    this.points == other.points &&
    this.neighborhoods == other.neighborhoods &&
    this.searchSize == other.searchSize &&
    this.degreeBound == other.degreeBound &&
    this.alpha == other.alpha &&
    this.maxIndexSize == other.maxIndexSize
  }

  private def robustPrune(
    point: Int,
    candidates: Vector[Int]
  ): mutable.PriorityQueue[Neighbor] = {
    val candidateIndexes = candidates ++ neighborhoods(point).map({
      case (idx, dist) => idx
    })
    val candidatePairFutures = Future.sequence(
      candidateIndexes.map(idx =>
        Future {
          (idx, distance(points(point), points(idx)))
        }
      )
    )
    val candidatePairs = Await.result(candidatePairFutures, Duration.Inf)

    val candidateExtended = mutable.PriorityQueue.empty[(Int, Float)](
      Ordering.by[Neighbor, Float](_._2)
    )
    candidatePairs.foreach(candidateExtended.enqueue(_))

    val newNeighborhood =
      mutable.PriorityQueue.empty[Neighbor](Ordering.by[Neighbor, Float](_._2))

    while (candidateExtended.nonEmpty) {
      val (pStarIdx, pStarDist) = candidateExtended.dequeue()
      newNeighborhood.enqueue((pStarIdx, pStarDist))
      if (newNeighborhood.size >= degreeBound) {
        return newNeighborhood
      }

      candidateExtended.filter({ case (pPrimeIdx, pPrimeDist) =>
        alpha * distance(points(pStarIdx), points(pPrimeIdx)) > pPrimeDist
      })
    }

    newNeighborhood
  }

  private def greedySearch(
    query: Point,
    k: Int
  ): (Array[Neighbor], Vector[Int]) = {
    var beam =
      mutable.PriorityQueue.empty[Neighbor](Ordering.by[Neighbor, Float](_._2))
    beam.enqueue((startingIdx, distance(points(startingIdx), query)))
    val visited = mutable.Set.empty[Int]

    while (beam.nonEmpty) {
      val (pStarIdx, pStarDist) = beam.dequeue()

      neighborhoods(pStarIdx).foreach({ case (pPrimeIdx, pPrimeDist) =>
        if (!visited.contains(pPrimeIdx)) {
          beam.enqueue((pPrimeIdx, distance(points(pPrimeIdx), query)))
        }
      })
      visited += pStarIdx

      if (beam.size >= searchSize) {
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
      this.points = newPoints.toVector

      // initializes G as a random R-regular graph
      val rng = new scala.util.Random()
      val neighborhoodFutures = Future.sequence(
        this.points.indices
          .zip(this.points)
          .map({ case (idx, point) =>
            Future {
              val neighborhood = mutable.PriorityQueue
                .empty[Neighbor](Ordering.by[Neighbor, Float](_._2))
              while (neighborhood.size < degreeBound) {
                val candidateIdx = rng.nextInt(this.points.size)
                if (
                  candidateIdx != idx && !neighborhood.exists({
                    case (neighborIdx, _) => neighborIdx == candidateIdx
                  })
                ) {
                  neighborhood.enqueue(
                    (candidateIdx, distance(point, this.points(candidateIdx)))
                  )
                }
              }
              neighborhood
            }
          })
      )
      this.neighborhoods =
        Await.result(neighborhoodFutures, Duration.Inf).toVector
      println("Random graph initialized")
      this.startingIdx = computeMedoidIdx(this.points)
      println("Medoid computed")

      this.points.indices
        .zip(this.points)
        .foreach({ case (idx, point) =>
          val (candidates, path) = greedySearch(point, 1)
          this.neighborhoods =
            this.neighborhoods.updated(idx, robustPrune(idx, path))

          this
            .neighborhoods(idx)
            .foreach({ case (neighborIdx, neighborDist) =>
              val neighborIndexes = this
                .neighborhoods(neighborIdx)
                .map({ case (idx, _) => idx })
                .toSet
              if ((neighborIndexes + idx).size > degreeBound) {
                val neighborhoodToPrune = this
                  .neighborhoods(neighborIdx)
                  .map({ case (idx, _) => idx })
                  .toVector :+ idx
                val prunedNeighborhood =
                  robustPrune(neighborIdx, neighborhoodToPrune)
              } else {
                this.neighborhoods(neighborIdx).enqueue((idx, neighborDist))
              }
            })
        })
    } else {
      val startingN = this.points.size
      this.points = this.points ++ newPoints

      newPoints.indices
        .zip(newPoints)
        .foreach({ case (idxSurplus, point) =>
          val (candidates, path) = greedySearch(point, 1)
          val prunedCandidates   = robustPrune(startingN + idxSurplus, path)

          prunedCandidates.foreach({ case (neighborIdx, neighborDist) =>
            this
              .neighborhoods(neighborIdx)
              .enqueue((startingN + idxSurplus, neighborDist))

            if (this.neighborhoods(neighborIdx).size > degreeBound) {
              val neighborhoodToPrune = this
                .neighborhoods(neighborIdx)
                .map({ case (idx, _) => idx })
                .toVector :+ (startingN + idxSurplus)
              val prunedNeighborhood =
                robustPrune(neighborIdx, neighborhoodToPrune)
              this.neighborhoods =
                this.neighborhoods.updated(neighborIdx, prunedNeighborhood)
            }
          })
        })
    }
  }

  override def remove(index: Int): Unit = ???

  override def search(query: Point, k: Int): Array[Neighbor] = {
    val (result, _) = greedySearch(query, k)
    result
  }

  override def saveIndex(path: String): Unit = {
    val oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(path))
    oos.writeObject(this)
    oos.close()
  }
}
