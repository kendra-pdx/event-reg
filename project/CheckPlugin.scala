import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbt.{Def, _}

object CheckPlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val check = taskKey[Any]("run checks")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = {
    check := Def.sequential(
      undeclaredCompileDependencies,
      unusedCompileDependencies
    ).value
  }
}
