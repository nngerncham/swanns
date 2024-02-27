package muic.nawat.senior.rmcomp
package models

class DiskANNNearestReplace(
  override val searchSize: Int,
  override val degreeBound: Int,
  override val alpha: Float,
  override val maxIndexSize: Int
) extends DiskANNBase(searchSize, degreeBound, alpha, maxIndexSize) {

  private def remove(index: Int): Unit = {
    // searches for the nearest neighbor
    val nnResult = super.greedySearch(this.points(index), 1)
    val nearestIndex = {
      nnResult match
        case (indexes, _) => indexes(0)._1
    }

    // update the neighborhood of the nearest neighbor to include it and include original one
    val nearestNeighborhood = this.neighborhoods(nearestIndex)
    nearestNeighborhood.foreach(neighborId => {
      val newNbrhood = this.neighborhoods(neighborId) + nearestIndex - index
      this.neighborhoods = this.neighborhoods.updated(neighborId, newNbrhood)
    })

    // combine neighborhood of NN to index's and prune if needed
    var newNN = nearestNeighborhood ++ this.neighborhoods(index) - index
    if (newNN.size > this.degreeBound) {
      newNN = super.robustPrune(nearestIndex, newNN.toVector)
    }
    this.neighborhoods = this.neighborhoods.updated(nearestIndex, newNN)

    // marks the index as deleted
    this.neighborhoods = this.neighborhoods.updated(index, Set.empty)
    this.deleted = this.deleted + index
  }

  override def batchRemove(indexes: Seq[Int]): Unit = {
    indexes.foreach(remove)
  }
}

object DiskANNNearestReplace {
  def loadIndex(path: String): DiskANNNearestReplace = {
    val ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))
    val baseIndex = ois.readObject().asInstanceOf[DiskANNBase]
    ois.close()

    val index = new DiskANNNearestReplace(
      baseIndex.searchSize,
      baseIndex.degreeBound,
      baseIndex.alpha,
      baseIndex.maxIndexSize
    )
    index.startingIdx = baseIndex.startingIdx
    index.points = baseIndex.points
    index.neighborhoods = baseIndex.neighborhoods
    index.deleted = baseIndex.deleted
    // tracker attached later

    index
  }
}
