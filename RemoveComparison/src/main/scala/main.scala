package muic.nawat.senior.rmcomp

import models.{DiskANNBase, Tracker, computeGroundTruths, readFvecIntoPoints}

@main
def main(): Unit = {
//  val siftSmall = readFvecIntoPoints(
//    "/home/nawat/muic/senior/data/siftsmall/base.fvecs"
//  ).take(500)
  val siftSmall = readFvecIntoPoints(
    "/home/nawat/muic/senior/data/siftsmall/base.fvecs"
  )
  val tracker = new Tracker()
  val builder = new DiskANNBase(64, 32, 1.2, tracker, 10_000)
  builder.batchAdd(siftSmall)
  builder.saveIndex(
    "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall500.index"
  )

  val siftSmallQuery = readFvecIntoPoints(
    "/home/nawat/muic/senior/data/siftsmall/query.fvecs"
  ) // 100 queries

  val kToTest = 20
  val index = DiskANNBase.loadIndex(
    "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall500.index"
  )
  val annsSearchResults = siftSmallQuery.map(index.search(_, kToTest))

  println("Computing ground truth")
  val siftSmallGT = computeGroundTruths(siftSmall, siftSmallQuery, kToTest)
  val groundTruthOutput = new java.io.ObjectOutputStream(
    new java.io.FileOutputStream(
      "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall500.gt"
    )
  )
  groundTruthOutput.writeObject(siftSmallGT)
  groundTruthOutput.close()
//  val siftSmallGT = {
//    val groundTruthInput = new java.io.ObjectInputStream(
//      new java.io.FileInputStream(
//        "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall500.gt"
//      )
//    )
//    val result =
//      groundTruthInput.readObject().asInstanceOf[Vector[Array[(Int, Double)]]]
//    groundTruthInput.close()
//    result
//  }

  println("Computing recall")
  val siftSmallRecalls =
    annsSearchResults
      .zip(siftSmallGT)
      .map({ case (anns, gt) =>
        val annsSet = anns.map({ case (i, _) => i }).toSet
        val gtSet   = gt.map({ case (i, _) => i }).toSet

//        println((annsSet intersect gtSet).size)
//        (annsSet intersect gtSet).size
        (annsSet intersect gtSet).size.toDouble / kToTest.toDouble
      })
  val recall = siftSmallRecalls.sum / siftSmallRecalls.size.toDouble
  println(s"Recall@$kToTest: $recall")
}
