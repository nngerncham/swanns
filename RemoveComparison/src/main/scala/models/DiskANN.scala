package muic.nawat.senior.rmcomp
package models

import scala.collection.mutable

class DiskANN(searchSize: Int,
              degreeBound: Int,
              alpha: Float,
              tracker: Tracker) extends ANNSGraph {
  private var startingIdx: Int = -1
  private var points: Vector[Point] = Vector.empty
  private var neighborhoods: Vector[mutable.PriorityQueue[Neighbor]] = Vector.empty

  private def robustPrune(point: Int, candidates: Vector[Int]): mutable.PriorityQueue[Neighbor] = {
    val candidateIndexes = candidates ++ neighborhoods(point).map({ case (idx, dist) => idx })
    val candidatePairs = candidateIndexes.map(idx => (idx, distance(points(point), points(idx))))
    val candidateExtended = mutable.PriorityQueue.empty[(Int, Float)](Ordering.by[Neighbor, Float](_._2))
    candidatePairs.foreach(candidateExtended.enqueue(_))

    val newNeighborhood = mutable.PriorityQueue.empty[Neighbor](Ordering.by[Neighbor, Float](_._2))

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

  private def greedySearch(query: Point, k: Int): (Array[Neighbor], Vector[Int]) = {
    var beam = mutable.PriorityQueue.empty[Neighbor](Ordering.by[Neighbor, Float](_._2))
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

  override def batchAdd(points: Array[Point]): Unit = ???

  override def remove(index: Int): Unit = ???

  //  override def batchRemove(indexes: Array[Int]): Unit = ???

  override def search(query: Point, k: Int): Array[Neighbor] = {
    val (result, _) = greedySearch(query, k)
    result
  }
}
