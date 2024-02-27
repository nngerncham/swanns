package muic.nawat.senior.rmcomp
package models

type Point = Array[Float]

trait ANNSGraph {
  def batchAdd(newPoints: Seq[Point]): Unit
  def search(query: Point, k: Int): Array[(Int, Float)]
  def saveIndex(path: String): Unit
  def batchRemove(indexes: Seq[Int]): Unit // can be a for loop of removals
}
