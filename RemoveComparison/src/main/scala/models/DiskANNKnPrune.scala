package muic.nawat.senior.rmcomp
package models

class DiskANNKnPrune(
  override val searchSize: Int,
  override val degreeBound: Int,
  override val alpha: Float,
  override val maxIndexSize: Int
) extends DiskANNBase(searchSize, degreeBound, alpha, maxIndexSize) {
  private def remove(index: Int): Unit = {
    val nbrhood = this.neighborhoods(index)
    nbrhood.foreach(nbrIdx => {
      // connect all neighbors to each other to form partial K_n
      var expandedNbrhood =
        this.neighborhoods(nbrIdx) ++ this.neighborhoods(index) - index

      // prune if needed
      if (expandedNbrhood.size > this.degreeBound) {
        expandedNbrhood =
          sequentialRobustPrune(nbrIdx, expandedNbrhood.toVector)
      }
    })

    // remove the index
    this.neighborhoods = this.neighborhoods.updated(index, Set.empty)
    this.deleted = this.deleted + index
  }

  override def batchRemove(indexes: Seq[Int]): Unit = {
    indexes.foreach(remove)
  }
}

object DiskANNKnPrune {
  def loadIndex(path: String): DiskANNKnPrune = {
    val ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))
    val baseIndex = ois.readObject().asInstanceOf[DiskANNBase]
    ois.close()

    val index = new DiskANNKnPrune(
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
