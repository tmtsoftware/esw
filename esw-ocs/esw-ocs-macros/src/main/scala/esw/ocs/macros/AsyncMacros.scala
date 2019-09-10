package esw.ocs.macros

import scala.concurrent.ExecutionContext
import scala.reflect.macros.blackbox

object AsyncMacros {
  def async[T: c.WeakTypeTag](c: blackbox.Context)(body: c.Expr[T])(ec: c.Expr[ExecutionContext]): c.universe.Tree = {
    import c.universe._
    q"_root_.esw.ocs.impl.dsl.Async.async($body)($ec)"
  }

  def asyncStrand[T: c.WeakTypeTag](c: blackbox.Context)(body: c.Expr[T])(strandEc: c.Expr[StrandEc]): c.Tree = {
    import c.universe._
    val ec = reify(strandEc.splice.ec)
    q"_root_.esw.ocs.impl.dsl.Async.async($body)($ec)"
  }

  def await(c: blackbox.Context): c.universe.Tree = {
    import c.universe._
    val arg = c.prefix.tree.asInstanceOf[Apply].args.head
    q"_root_.esw.ocs.impl.dsl.Async.await($arg)"
  }
}
