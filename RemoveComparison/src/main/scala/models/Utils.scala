package muic.nawat.senior.rmcomp
package models

def distance(p1: Point, p2: Point): Float =
  Math.sqrt(p1.zip(p2).map({ case (a, b) => Math.pow(a - b, 2) }).sum).toFloat
