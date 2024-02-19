package muic.nawat.senior.rmcomp
package models

type Point = Array[Float]
type Neighbor = (Int, Float)


trait ANNSGraph {
  def batchAdd(newPoints: Seq[Point]): Unit

  def remove(index: Int): Unit

  //  def batchRemove(indexes: Array[Int]): Unit

  def search(query: Point, k: Int): Array[Neighbor]

  def saveIndex(path: String): Unit
}
