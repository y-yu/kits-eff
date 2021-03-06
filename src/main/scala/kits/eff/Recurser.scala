package kits.eff

import scala.annotation.tailrec

trait Recurser[F, R, A, B] {
  def pure(a: A): Eff[R, B]

  def tailRec[T](fa: F with Fx[T]): Either[T, Eff[R, B]]

  def apply(eff: Eff[F with R, A])(implicit F: Manifest[F]): Eff[R, B] = {
    @tailrec
    def loop(eff: Eff[F with R, A]): Eff[R, B] =
      eff match {
        case Eff.Pure(a) =>
          pure(a)
        case Eff.Impure(u, k) =>
          u.decomp[F, R] match {
            case Right(fa) =>
              tailRec(fa) match {
                case Left(a) =>
                  loop(k(a))
                case Right(eff) =>
                  eff
              }
            case Left(r) =>
              Eff.Impure(r, Arrs((a: Any) => apply(k(a))))
          }
      }
    loop(eff)
  }
}

trait StateRecurser[F, R, S, A, B] {
  def pure(s: S, a: A): Eff[R, B]

  def tailRec[T](s: S, fa: F with Fx[T]): Either[(S, T), Eff[R, B]]

  def apply(s: S, eff: Eff[F with R, A])(implicit F: Manifest[F]): Eff[R, B] = {
    @tailrec
    def loop(s: S, eff: Eff[F with R, A]): Eff[R, B] =
      eff match {
        case Eff.Pure(a) =>
          pure(s, a)
        case Eff.Impure(u, k) =>
          u.decomp[F, R] match {
            case Right(fa) =>
              tailRec(s, fa) match {
                case Left((s, a)) =>
                  loop(s, k(a))
                case Right(eff) =>
                  eff
              }
            case Left(r) =>
              Eff.Impure(r, Arrs((a: Any) => apply(s, k(a))))
          }
      }
    loop(s, eff)
  }
}
