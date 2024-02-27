package muic.nawat.senior.rmcomp
package models

class DiskANNLazyDelete(
  override val searchSize: Int,
  override val degreeBound: Int,
  override val alpha: Float,
  override val maxIndexSize: Int
) extends DiskANNBase(searchSize, degreeBound, alpha, maxIndexSize) {
  private def remove(index: Int): Unit = {
    // remove the index
    this.neighborhoods = this.neighborhoods.updated(index, Set.empty)
    this.deleted = this.deleted + index
  }

  override def batchRemove(indexes: Seq[Int]): Unit = {
    indexes.foreach(remove)
  }
}

object DiskANNLazyDelete {
  def loadIndex(path: String): DiskANNBase = {
    val ois = new java.io.ObjectInputStream(new java.io.FileInputStream(path))
    val baseIndex = ois.readObject().asInstanceOf[DiskANNBase]
    ois.close()

    val index = new DiskANNLazyDelete(
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
