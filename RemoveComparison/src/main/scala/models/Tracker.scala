package muic.nawat.senior.rmcomp
package models

class Tracker(var distComparisons: Int,
              var pathLengths: Int,
              var recall: Float,
              var qps: Int) {
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
