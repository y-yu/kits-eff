package kits.eff

trait Interpreter[F, R, A, B] {
  def pure(a: A): Eff[R, B]

  def flatMap[T](ft: F with Fx[T])(f: T => Eff[R, B]): Eff[R, B]

  def apply(eff: Eff[F with R, A])(implicit F: Manifest[F]): Eff[R, B] =
    eff match {
      case Eff.Pure(a) =>
        pure(a)
      case Eff.Impure(u, k) =>
        u.decomp[F, R] match {
          case Right(fa) =>
            flatMap(fa)(a => apply(k(a)))
          case Left(r) =>
            Eff.Impure(r, Arrs((a: Any) => apply(k(a))))
        }
    }
}

trait ApplicativeInterpreter[F, R] {
  type Result[A]

  def pure[A](a: A): Eff[R, Result[A]]

  def flatMap[A, B](fa: F with Fx[A])(f: A => Eff[R, Result[B]]): Eff[R, Result[B]]

  def ap[A, B](fa: F with Fx[A])(f: Eff[R, Result[A => B]]): Eff[R, Result[B]]

  def map[A, B](ra: Result[A])(f: A => B): Result[B]

  def apply[A](eff: Eff[F with R, A])(implicit F: Manifest[F]): Eff[R, Result[A]] =
    eff match {
      case Eff.Pure(a) =>
        pure(a)
      case Eff.Impure(u, k) =>
        u.decomp[F, R] match {
          case Right(fa) =>
            k match {
              case Arrs.LeafA(k) =>
                ap(fa)(apply(k))
              case _ =>
                flatMap(fa)(a => apply(k(a)))
            }
          case Left(r) =>
            k match {
              case Arrs.LeafA(k) =>
                Eff.Impure(r, Arrs(apply(k).map(r => (a: Any) => map(r)(_(a)): Result[A])))
              case _ =>
                Eff.Impure(r, Arrs((a: Any) => apply[A](k(a))))
            }
        }
    }
}
