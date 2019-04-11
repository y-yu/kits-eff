package kits.eff

import scala.annotation.tailrec

sealed abstract class Arrs[-R, A, B] extends Product with Serializable {
  def :+[S, C](f: B => Eff[S, C]): Arrs[R with S, A, C] = Arrs.Node(this, Arrs.Leaf(f))

  def ++[S, C](that: Arrs[S, B, C]): Arrs[R with S, A, C] = Arrs.Node(this, that)

  def apply(value: A): Eff[R, B] = {
    @tailrec
    def loop[A](value: A, arrs: Arrs[R, A, B]): Eff[R, B] =
      arrs match {
        case Arrs.Leaf(f) => f(value)
        case Arrs.Node(Arrs.Leaf(f), r) =>
          f(value) match {
            case Eff.Pure(v) => loop(v, r)
            case Eff.Impure(u, k) =>
              Eff.Impure(u, k ++ r)
            case Eff.ImpureAp(u, k) =>
              Eff.Impure(u, Arrs.Leaf((a: Any) => k.map(_(a))) ++ r)
          }
        case Arrs.Node(Arrs.Node(ll, lr), r) =>
          loop(value, Arrs.Node(ll, Arrs.Node(lr, r)))
      }
    loop(value, this)
  }
}

object Arrs {
  case class Leaf[-R, A, B](value: A => Eff[R, B]) extends Arrs[R, A, B]

  case class Node[-R, A, B, C](left: Arrs[R, A, B], right: Arrs[R, B, C]) extends Arrs[R, A, C]
}
