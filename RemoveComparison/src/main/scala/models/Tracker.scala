package muic.nawat.senior.rmcomp
package models

class Tracker extends Serializable {
  private var distComparisons: Int = 0
  private var pathLengths: Int     = 0
  private var recall: Float        = 0
  private var qps: Int             = 0

  def addDistComps(n: Int): Unit = {
    distComparisons = distComparisons + n
  }

  def addPathLengths(n: Int): Unit = {
    pathLengths = pathLengths + n
  }

  def addRecall(x: Float): Unit = {
    recall = recall + x
  }

  def addQps(n: Int): Unit = {
    qps = qps + n
  }
}
