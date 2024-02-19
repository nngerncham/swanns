package muic.nawat.senior.rmcomp

import models.{DiskANN, Tracker, readFvecIntoPoints, computeMedoidIdx}

@main
def main(): Unit = {
  val siftSmall = readFvecIntoPoints(
    "/home/nawat/muic/senior/data/siftsmall/base.fvecs"
  ).take(50)

  val tracker = new Tracker()
  // val index   = new DiskANN(200, 70, 2.0, tracker, 10_000)
  val index = new DiskANN(16, 8, 1.0, tracker, 10_000)
  index.batchAdd(siftSmall)
  // println(computeMedoidIdx(siftSmall))
  index.saveIndex(
    "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall.index"
  )

  val index2 = new DiskANN(
    "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall.index"
  )

  println("" + index.searchSize + " " + index2.searchSize)
  println("" + index.degreeBound + " " + index2.degreeBound)
  println("" + index.alpha + " " + index2.alpha)
  println("" + index.maxIndexSize + " " + index2.maxIndexSize)

  println("" + index.startingIdx + " " + index2.startingIdx)
  println("" + index.startingIdx + " " + index2.startingIdx)

  index.points
    .zip(index2.points)
    .foreach({ case (a, b) =>
      println("" + a + "," + b)
    })
  index.neighborhoods
    .zip(index2.neighborhoods)
    .foreach({ case (a, b) =>
      println("" + a + "," + b)
    })
}
