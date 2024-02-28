package muic.nawat.senior.rmcomp

import models.{
  DiskANNBase,
  DiskANNKnPrune,
  DiskANNNearestReplace,
  DiskANNLazyDelete,
  Tracker,
  computeGroundTruths,
  computeRecall,
  readFvecIntoPoints
}

@main
def main(): Unit = {
  val size    = 10_000
  val rmRatio = 0.1
  val remove  = (size * rmRatio).toInt
  val keep    = size - remove
  println(s"Size: $size, Remove: $remove, Keep: $keep")

  println("Building index")
  val indexPath =
    "/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall100.index"
  val siftSmall = readFvecIntoPoints(
    "/home/nawat/muic/senior/data/siftsmall/base.fvecs"
  ).take(size)
//  val tracker = new Tracker()
//  val builder = new DiskANNBase(64, 32, 1.2, size)
//  builder.batchAdd(siftSmall)
//  builder.saveIndex(indexPath)

  val kToTest = 10
  val queries = readFvecIntoPoints(
    "/home/nawat/muic/senior/data/siftsmall/query.fvecs"
  )

  println("Searching on original index")
  val nearestReplaceIndex  = DiskANNBase.loadIndex(indexPath)
  val nearestReplaceResult = queries.map(nearestReplaceIndex.search(_, kToTest))
  println("Computing original ground truth")
  val gt = computeGroundTruths(siftSmall, queries, kToTest)
  val gtOut = new java.io.ObjectOutputStream(
    new java.io.FileOutputStream(
      s"/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall$size.gt"
    )
  )
  gtOut.writeObject(gt)
  gtOut.close()
//  val gt = {
//    val groundTruthInput = new java.io.ObjectInputStream(
//      new java.io.FileInputStream(
//        s"/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall$size.gt"
//      )
//    )
//    val result =
//      groundTruthInput.readObject().asInstanceOf[Vector[Array[(Int, Double)]]]
//    groundTruthInput.close()
//    result
//  }
  println("Computing original recall")
  val originalRecall = computeRecall(gt, nearestReplaceResult)
  println(s"Original recall@$kToTest: $originalRecall")

  println("Searching on removed index replacing with NN")
  val nnReplaceIndex = DiskANNNearestReplace.loadIndex(indexPath)
  nnReplaceIndex.batchRemove((0 until remove).toVector)
  val nnReplaceResult = queries.map(nnReplaceIndex.search(_, kToTest))
  println("Searching on removed index complete then prune")
  val knPruneIndex = DiskANNKnPrune.loadIndex(indexPath)
  knPruneIndex.batchRemove((0 until remove).toVector)
  val knPruneResult = queries.map(knPruneIndex.search(_, kToTest))
  println("Searching on removed index lazy delete")
  val lazyIndex = DiskANNLazyDelete.loadIndex(indexPath)
  lazyIndex.batchRemove((0 until remove).toVector)
  val lazyResult = queries.map(lazyIndex.search(_, kToTest))
  println("Removed searches done")

  println("Computing removed ground truth")
  val removedGT =
    computeGroundTruths(siftSmall.takeRight(keep), queries, kToTest)
  new java.io.FileOutputStream(
    s"/home/nawat/muic/senior/swanns/RemoveComparison/builtIndex/siftsmall$size.rgt"
  )
  println("Computing removed recall")
  val nnReplaceRecall = computeRecall(removedGT, nnReplaceResult)
  println(s"NN Replacement recall@$kToTest: $nnReplaceRecall")
  val knPruneRecall = computeRecall(removedGT, knPruneResult)
  println(s"Kn+Prune recall@$kToTest: $knPruneRecall")
  val lazyRecall = computeRecall(removedGT, lazyResult)
  println(s"Lazy delete recall@$kToTest: $lazyRecall")
}
